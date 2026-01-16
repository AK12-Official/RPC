package Server.serviceRegister;

import java.net.InetSocketAddress;

/**
 * ClassName：ServiceRegister
 * Package: Server.serviceRegister
 *
 * @ Author：zh
 * @ Create: 2026/1/16 15:01
 * @ Version: 1.0
 * @ Description: 服务注册接口
 */
public interface ServiceRegister {
    //  注册：保存服务与地址。
    void register(String serviceName, InetSocketAddress serviceAddress);

}
