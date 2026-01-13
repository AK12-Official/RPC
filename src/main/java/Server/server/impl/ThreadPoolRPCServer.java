package Server.server.impl;

import Server.provider.ServiceProvider;
import Server.server.RpcServer;
import Server.server.work.WorkThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ClassName：ThreadPoolRPCServer
 * Package: Server.server.impl
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:39
 * @ Version: 1.0
 * @ Description:
 */
public class ThreadPoolRPCServer implements RpcServer {
    private final ThreadPoolExecutor threadPool;
    private ServiceProvider serviceProvider;

    public ThreadPoolRPCServer(ServiceProvider serviceProvider) {
        threadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                1000, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        this.serviceProvider = serviceProvider;
    }

    public ThreadPoolRPCServer(ServiceProvider serviceProvider, int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               BlockingQueue<Runnable> workQueue) {

        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void start(int port) {
        System.out.println("RPC Server starting...");
        try {
            ServerSocket serverSocket = new ServerSocket();
            while (true){
                Socket socket= serverSocket.accept();
                threadPool.execute(new WorkThread(socket,serviceProvider));
            }
        }catch(IOException  e){
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {

    }
}
