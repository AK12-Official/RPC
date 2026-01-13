package Server.server.work;

import Server.provider.ServiceProvider;
import common.Message.RpcRequest;
import common.Message.RpcResponse;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * ClassName：WorkThread
 * Package: Server.server.work
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:50
 * @ Version: 1.0
 * @ Description: 负责启动线程和客户端进行数据传输
 */
@AllArgsConstructor
public class WorkThread implements Runnable{
    private Socket socket;
    private ServiceProvider serviceProvider;

    @Override
    public void run() {
        try{
            ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
            //读取客户端传来的request
            RpcRequest rpcRequest=(RpcRequest) ois.readObject();
            //反射调用服务方法获取返回值
            RpcResponse rpcResponse=getResponse(rpcRequest);
            //向客户端写入response
            oos.writeObject(rpcResponse);
            oos.flush();
        }catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    //解析收到的request信息，寻找服务进行调用并返回结果
    private RpcResponse getResponse(RpcRequest rpcRequest){
        //得到服务名
        String interfaceName=rpcRequest.getInterfaceName();
        //得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);

        //反射调用方法
        Method method=null;
        try {
            method= service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamsType());
            Object invoke=method.invoke(service,rpcRequest.getParams());
            return RpcResponse.sussess(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RpcResponse.fail();
        }
    }
}
