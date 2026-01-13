package Server.server.impl;

import Server.provider.ServiceProvider;
import Server.server.RpcServer;
import Server.server.work.WorkThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
 *  使用线程池：threadPool.execute(...)
 *  线程数量可控：核心线程数 = CPU 核心数，最大线程数 = 1000
 *  线程复用，减少创建/销毁开销
 *  支持自定义线程池参数
 *
 *  默认参数：
 *      核心线程数：Runtime.getRuntime().availableProcessors()（CPU 核心数）
 *      最大线程数：1000
 *      空闲线程存活时间：60 秒
 *      工作队列：ArrayBlockingQueue<>(100)（容量 100）
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
            ServerSocket serverSocket = new ServerSocket(port);
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
