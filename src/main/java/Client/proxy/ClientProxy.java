package Client.proxy;

import Client.IOClient;
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
 * @ Description:
 */
@AllArgsConstructor
public class ClientProxy implements InvocationHandler {
    //传入参数service接口的class对象，反射封装成一个request
    private String host;
    private int port;

    //jdk动态代理，每一次代理对象调用方法，都会经过此方法增强（反射获取request对象，socket发送到服务端）
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //构建Request
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())    //？？？为什么要用method点
                .methodName(method.getName())
                .params(args).paramsType(method.getParameterTypes())
                .build();

        //IOClient.sendRequest 和服务端进行数据传输
        RpcResponse response = IOClient.sendRequest(host, port, rpcRequest);
        return response.getData();
    }

    public <T> T getProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T) o;       //???这是在干嘛
    }


}
