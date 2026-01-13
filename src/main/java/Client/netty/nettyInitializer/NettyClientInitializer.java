package Client.netty.nettyInitializer;

import Client.netty.handler.NettyClientHandler;
import common.Message.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.AttributeKey;

/**
 * ClassName：NettyClientInitializer
 * Package: Client.netty.nettyInitializer
 *
 * @ Author：zh
 * @ Create: 2026/1/14 10:04
 * @ Version: 1.0
 * @ Description:
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //消息格式 【长度】【消息体】，解决沾包问题
        //netty默认底层通过TCP 进行传输，TCP是面向流的协议，接收方在接收到数据时无法直接得知一条消息的具体字节数，不知道数据的界限。由于TCP的流量控制机制，发生沾包或拆包，会导致接收的一个包可能会有多条消息或者不足一条消息，从而会出现接收方少读或者多读导致消息不能读完全的情况发生
        //在发送消息时，先告诉接收方消息的长度，让接收方读取指定长度的字节，就能避免这个问题
        pipeline.addLast(
                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
        //计算当前待发送消息的长度 写入到前四个字节中
        pipeline.addLast(new LengthFieldPrepender(4));
        //编码器
        //使用Java序列化方法 netty自带的解码编码支持传输这种结构
        pipeline.addLast(new ObjectEncoder());
        //解码器
        //使用Netty中的ObjectDecoder，它用于将字节流解码为Java对象
        //在ObjectDecoder中的构造函数中传入了一个ClassResolver对象，用于解析类名并加载相应的类
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));

        pipeline.addLast(new NettyClientHandler());
    }
}
