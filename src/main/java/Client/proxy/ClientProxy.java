package Client.proxy;

import Client.rpcClient.RpcClient;
import Client.rpcClient.impl.NettyRpcClient;
import common.Message.RpcRequest;
import common.Message.RpcResponse;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * ClassName：ClientProxy
 * Package: Client.proxy
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:00
 * @ Version: 1.0
 * @ Description: 负责创建代理对象，并实现代理对象的调用处理逻辑
 */ 
@AllArgsConstructor
public class ClientProxy implements InvocationHandler {
    //传入参数service接口的class对象，反射封装成一个request


    private RpcClient rpcClient;
    public ClientProxy(){
        rpcClient=new NettyRpcClient();
    }

    //jdk动态代理，每一次代理对象调用方法，都会经过此方法增强（反射获取request对象，socket发送到服务端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //构建Request
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsType(method.getParameterTypes())
                .build();

        //sendRequest 和服务端进行数据传输
        RpcResponse response = rpcClient.sendRequest(rpcRequest);
        return response.getData();
    }

    /**
     * 用于创建jdk动态代理对象，让客户端可以像调用本地方法一样调用远程服务
     *
     * @param <T>   
     * @param clazz 接口的 Class 对象（如 UserService.class）
     * @return 实现了接口 T 的代理对象
     */
    public <T> T getProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T) o;
    }


}
