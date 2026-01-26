package Server.server.impl;

import Server.provider.ServiceProvider;
import Server.server.RpcServer;
import Server.server.work.WorkThread;
import common.service.UserService;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ClassName：SimpleRPCServer
 * Package: Server.server.impl
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:33
 * @ Version: 1.0
 * @ Description:
 */
@Deprecated
@AllArgsConstructor
public class SimpleRPCServer implements RpcServer {
    private static final boolean DEBUG_LOG = false;

    private ServiceProvider serviceProvider;

    @Override
    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            if (DEBUG_LOG) {
                System.out.println("服务器启动了");
            }
            while (true) {
                //如果没有连接，会堵塞在这里
                Socket socket = serverSocket.accept();
                //有连接，创建一个新的线程执行处理
                new Thread(new WorkThread(socket, serviceProvider)).start();
            }
        } catch (IOException e) {
            if (DEBUG_LOG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {

    }
}
