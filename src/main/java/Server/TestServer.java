package Server;

import Server.provider.ServiceProvider;
import Server.server.RpcServer;
import Server.server.impl.NettyRPCServer;
import common.service.Impl.UserServiceImpl;
import common.service.UserService;

/**
 * ClassName：TestServer
 * Package: Server
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:31
 * @ Version: 1.0
 * @ Description:
 */
public class TestServer {
    public static void main(String[] args) {
        UserService userService=new UserServiceImpl();

        ServiceProvider serviceProvider=new ServiceProvider("127.0.0.1",8888);
        serviceProvider.provideServiceInterface(userService,true);

        RpcServer rpcServer=new NettyRPCServer(serviceProvider);
        rpcServer.start(8888);
    }
}
