package Server;

import Server.provider.ServiceProvider;
import Server.server.RpcServer;
import Server.server.impl.NettyRPCServer;
import common.service.Impl.UserServiceImpl;
import common.service.UserService;

/**
 * ClassName：AnotherServer
 * Package: Server
 *
 * @ Author：zh
 * @ Create: 2026/1/25 16:42
 * @ Version: 1.0
 * @ Description:NULL
 */
public class AnotherServer {
    public static void main(String[] args) {
        UserService userService=new UserServiceImpl();

        ServiceProvider serviceProvider=new ServiceProvider("127.0.0.1",9999);
        serviceProvider.provideServiceInterface(userService,true);

        RpcServer rpcServer=new NettyRPCServer(serviceProvider);
        rpcServer.start(9999);
    }
}