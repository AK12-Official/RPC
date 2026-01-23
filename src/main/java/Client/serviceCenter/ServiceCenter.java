package Client.serviceCenter;

import java.net.InetSocketAddress;

/**
 * ClassName：ServiceCenter
 * Package: Client.serviceCenter
 *
 * @ Author：zh
 * @ Create: 2026/1/16 13:32
 * @ Version: 1.0
 * @ Description: 服务中心
 */
public interface ServiceCenter {
    //  查询：根据服务名查找地址
    InetSocketAddress serviceDiscovery(String serviceName);
    //判断是否可重试
    boolean checkRetry(String serviceName) ;
}
