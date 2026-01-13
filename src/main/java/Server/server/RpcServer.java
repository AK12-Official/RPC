package Server.server;

/**
 * ClassName：RpcServer
 * Package: Server.server
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:32
 * @ Version: 1.0
 * @ Description:
 */
public interface RpcServer {
    //开启监听
    void start(int port);
    void stop();
}
