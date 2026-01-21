# Netty 编解码器与序列化器机制说明（本项目）

本文基于当前仓库实现，说明 **自定义 Netty 编解码器（`Encoder`/`Decoder`）** 与 **序列化器（`Serializer` 及其实现）** 的设计原理、消息格式（线协议）、执行流程，以及它们如何在客户端/服务端的 pipeline 中配合完成一次 RPC 调用。

---

## 1. 这套机制解决什么问题

RPC 需要在网络上传输“结构化对象”，典型是：

- 请求：`RpcRequest`（接口名、方法名、参数值、参数类型）
- 响应：`RpcResponse`（状态码、消息、返回值类型、返回值数据）

网络只能传 `byte[]`。所以需要两层转换：

1) **序列化层**：对象 ⇄ 字节数组（`Serializer` 负责）
2) **传输编解码层**：字节数组 ⇄ Netty `ByteBuf` + 额外消息头（`Encoder`/`Decoder` 负责）

这两个层次拆开后带来的直接收益：

- 可以在不改 Netty pipeline 的情况下切换序列化协议（Java 原生 / JSON / 以后扩展 Protobuf 等）
- 编解码器统一处理“消息头、长度、类型”，业务 handler 只关心 `RpcRequest`/`RpcResponse`

---

## 2. 消息格式（线协议 / 协议头）

本项目自定义协议在发送时由 `Encoder` 写入，接收时由 `Decoder` 读取。

### 2.1 字段顺序与字节长度

协议头 + body 的结构如下：

| 字段 | 类型 | 字节数 | 作用 |
|---|---:|---:|---|
| `messageType` | `short` | 2 | 区分请求/响应 |
| `serializerType` | `short` | 2 | 指定 body 用哪种序列化方式 |
| `length` | `int` | 4 | body 字节数组长度 |
| `body` | `byte[]` | N | 序列化后的对象数据 |

也就是：

```
[2B messageType][2B serializerType][4B length][N bytes body]
```

### 2.2 messageType 的含义

枚举 `MessageType`：

- `REQUEST`：code = 0
- `RESPONSE`：code = 1

`Encoder` 会根据消息对象类型写入：

- `RpcRequest` → `REQUEST(0)`
- `RpcResponse` → `RESPONSE(1)`

`Decoder` 根据读到的 `messageType` 决定反序列化目标类型（请求 or 响应）。

### 2.3 serializerType 的含义

`Serializer.getType()` 目前约定：

- 0：`ObjectSerializer`（Java 原生序列化）
- 1：`JsonSerializer`（Fastjson）

`Encoder` 把 `serializer.getType()` 写进 `serializerType` 字段。

`Decoder` 读取 `serializerType` 后调用：

- `Serializer.getSerializerByCode(serializerType)`

来选择对应序列化器，再反序列化 body。

---

## 3. Encoder：对象 → ByteBuf（发送方向）

`common.coder.Encoder` 继承自 `MessageToByteEncoder`，属于 **出站（outbound）handler**。

### 3.1 核心流程（对应代码顺序）

1) 写入 `messageType`
- 如果 `msg` 是 `RpcRequest` → 写 `REQUEST(0)`
- 如果 `msg` 是 `RpcResponse` → 写 `RESPONSE(1)`

2) 写入 `serializerType`
- 写 `serializer.getType()`

3) 序列化 body
- `byte[] serializeBytes = serializer.serialize(msg)`

4) 写入 `length`
- `out.writeInt(serializeBytes.length)`

5) 写入 `body`
- `out.writeBytes(serializeBytes)`

### 3.2 这一步为什么要写“消息头”

- `messageType`：让接收方知道这是“请求”还是“响应”，从而选择反序列化目标类型
- `serializerType`：让接收方知道 body 用哪种格式编码的（JSON / Java 对象流 …）
- `length`：让接收方明确 body 的边界，为“拆包/粘包”处理提供基础

---

## 4. Decoder：ByteBuf → 对象（接收方向）

`common.coder.Decoder` 继承自 `ByteToMessageDecoder`，属于 **入站（inbound）handler**。

### 4.1 核心流程（对应代码顺序）

1) 读 `messageType`（2B）
- 若不是 `0/1`，打印“不支持”并 return

2) 读 `serializerType`（2B）
- `Serializer serializer = Serializer.getSerializerByCode(serializerType)`
- 如果返回 null → 抛 `RuntimeException("不存在对应的序列化器")`

3) 读 `length`（4B）

4) 读 `body`（N bytes）
- `in.readBytes(bytes)`

5) 反序列化得到对象
- `Object deserialize = serializer.deserialize(bytes, messageType)`

6) 交给后续 handler
- `out.add(deserialize)`

### 4.2 重要的现实约束（半包/粘包）

当前 `Decoder` 的实现 **没有在读取前检查 `in.readableBytes()` 是否足够**。

在真实网络环境中，TCP 可能出现：

- 半包：一次 `channelRead` 只有部分字节（比如只有头，body 还没到）
- 粘包：多条消息挤在一次 `channelRead` 里

理论上这正是 `length` 字段存在的意义；但要正确处理，需要在 `decode()` 里做“足够字节才读取”的判断（不足就 return，等待下一批字节到来），或配合 Netty 的帧解码器（例如 `LengthFieldBasedFrameDecoder`）。

当前项目在演示/局域网环境可能“恰好没触发”，但这是后续工程化要优先补齐的点。

---

## 5. Serializer：对象 ⇄ byte[]（内容格式层）

`common.serializer.Serializer` 是序列化策略接口：

- `byte[] serialize(Object obj)`：对象 → 字节数组
- `Object deserialize(byte[] bytes, int messageType)`：字节数组 → 对象（需要 messageType 来区分 request/response）
- `int getType()`：返回序列化器编号（写入协议头）

并提供一个简单的工厂方法：

- `static Serializer getSerializerByCode(int code)`

### 5.1 ObjectSerializer（Java 原生序列化）

- `serialize()`：`ObjectOutputStream` 写入对象到 `ByteArrayOutputStream`
- `deserialize()`：`ObjectInputStream` 从 `ByteArrayInputStream` 读回对象
- `getType()`：0

特点：

- 优点：实现简单；反序列化不需要关心 messageType（字节流里包含类信息）
- 缺点：跨语言不友好；对类版本/serialVersionUID 变化敏感；通常性能与体积不如二进制协议

### 5.2 JsonSerializer（Fastjson）

- `serialize()`：`JSONObject.toJSONBytes(obj)`
- `getType()`：1

#### 5.2.1 为什么 JSON 反序列化要“类型回填”

JSON 本质是无类型/弱类型文本（即使这里是 bytes，本质仍是 JSON 数据）。

比如 `RpcRequest.params` 是 `Object[]`，JSON 解析后里面可能变成：

- 基本类型：`Integer/Long/String`（Fastjson 可直接还原）
- POJO：可能先变成 `JSONObject`，并不等于你的目标类

所以 `JsonSerializer.deserialize()` 做了两段关键逻辑：

**(1) 反序列化 `RpcRequest` 后，对 params 逐个校验类型**

- 读取 `request.getParamsType()[i]` 作为期望类型
- 若 `paramsType.isAssignableFrom(actual.getClass())` 不成立：
  - 说明当前 params[i] 的实际类型不对（常见是 `JSONObject`）
  - 用 `JSONObject.toJavaObject((JSONObject) request.getParams()[i], request.getParamsType()[i])` 转成正确类型

最终 `request.setParams(objects)`，保证 handler 反射调用时参数类型/值匹配。

**(2) 反序列化 `RpcResponse` 后，校验 data 的类型**

- `RpcResponse` 里携带了 `dataType`（这是本项目为了 JSON 反序列化额外加的）
- 若 `dataType` 与 `response.getData().getClass()` 不匹配：
  - 把 `data` 从 `JSONObject` 转回 `dataType`

这一步的意义是：客户端拿到 `RpcResponse.data` 时，不用再手工解析 JSON，而是直接得到“正确的 POJO/返回值类型”。

---

## 6. Netty pipeline：它们是怎么配合工作的

### 6.1 客户端 pipeline

客户端在 `NettyClientInitializer` 中的 pipeline：

1) `Decoder()`
2) `Encoder(new JsonSerializer())`
3) `NettyClientHandler()`

关键点：

- `Decoder` 是 inbound：负责把入站 ByteBuf → `RpcResponse`
- `Encoder` 是 outbound：负责把出站 `RpcRequest` → ByteBuf
- `NettyClientHandler` 是 inbound：拿到 `RpcResponse` 后放进 channel attribute，并关闭连接

客户端 handler 的处理方式：

- 收到响应后 `ctx.channel().attr(key).set(response)`
- 然后 `ctx.channel().close()`

这意味着当前实现是 **一次 RPC 调用/一次连接**（或至少一次响应后主动断开）。

### 6.2 服务端 pipeline

服务端在 `NettyServerInitializer` 中的 pipeline：

1) `Encoder(new JsonSerializer())`
2) `Decoder()`
3) `NettyRPCServerHandler(serviceProvider)`

看起来 encoder 在前、decoder 在后，但在 Netty 中：

- inbound 事件按“从前到后”经过 inbound handlers
- outbound 事件按“从后到前”经过 outbound handlers

而 `Encoder` 是 outbound、`Decoder` 是 inbound，因此两者相对顺序对“能否工作”影响不大（但为了可读性，通常会把 inbound 的 decoder 放前面，outbound 的 encoder 放后面）。

服务端 handler 的处理方式：

- 入站拿到 `RpcRequest`
- 通过 `serviceProvider.getService(interfaceName)` 找到实现类
- 反射调用 `method.invoke(service, params)`
- 封装 `RpcResponse.success(invoke)` 并 `ctx.writeAndFlush(response)`
- 然后 `ctx.close()`

同样体现：**一次请求处理完成就关闭连接**。

---

## 7. 一次完整 RPC 调用的端到端流程（串起来看）

### 7.1 客户端发送请求

1) 动态代理/客户端逻辑构造 `RpcRequest`
2) `channel.writeAndFlush(request)` 触发 outbound
3) `Encoder`：写入 `messageType=0`、`serializerType=1`、`length`、`body(JSON bytes)`
4) TCP 发送

### 7.2 服务端接收并处理请求

1) 收到字节 → inbound
2) `Decoder`：读出 `messageType=0`，选择 `RpcRequest.class` 反序列化
3) `JsonSerializer`：必要时把 `params` 从 `JSONObject` 回填成真实参数类型
4) `NettyRPCServerHandler`：根据接口名找到服务实现类，反射调用目标方法
5) 得到返回值，包装成 `RpcResponse`（可能携带 `dataType`）

### 7.3 服务端发送响应

1) `writeAndFlush(response)` → outbound
2) `Encoder`：写 `messageType=1` + `serializerType` + `length` + `body`
3) TCP 发送后关闭连接

### 7.4 客户端接收响应

1) `Decoder`：读出 `messageType=1`，选择 `RpcResponse.class` 反序列化
2) `JsonSerializer`：用 `dataType` 将 `data` 从 `JSONObject` 回填成真实返回值类型
3) `NettyClientHandler`：把 response 放入 channel attribute，关闭连接，供上层读取结果

---

## 8. 扩展与改造点（保持现有设计的前提下）

如果你后续要“更像一个可用的 RPC 框架”，通常会沿着这些方向演进：

1) **完善拆包/粘包处理**
- 在 `Decoder` 中加入 `readableBytes` 检查与“mark/reset readerIndex”
- 或引入 `LengthFieldBasedFrameDecoder`（本项目当前未使用）

2) **协议头增强**
- 增加 magic number / version / requestId（便于多路复用与排查问题）

3) **连接复用**
- 当前 client/server 都在处理完一次消息后 `close()`，吞吐会受限
- 可改为长连接 + requestId 匹配响应

4) **序列化器可插拔**
- 目前 `Serializer.getSerializerByCode()` 写死了 0/1
- 可改为 SPI/注册表方式

---

## 9. 你可以从哪些源码入口继续阅读

- 编解码：`common/coder/Encoder.java`、`common/coder/Decoder.java`
- 协议类型：`common/Message/MessageType.java`
- 消息体：`common/Message/RpcRequest.java`、`common/Message/RpcResponse.java`
- 序列化：`common/serializer/Serializer.java`、`ObjectSerializer.java`、`JsonSerializer.java`
- Pipeline：`Client/netty/nettyInitializer/NettyClientInitializer.java`、`Server/netty/nettyInitializer/NettyServerInitializer.java`
- 业务处理：`Server/netty/handler/NettyRPCServerHandler.java`、`Client/netty/handler/NettyClientHandler.java`
