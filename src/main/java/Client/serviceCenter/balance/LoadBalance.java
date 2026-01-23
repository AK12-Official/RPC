package Client.serviceCenter.balance;

import java.util.List;

/**
 * ClassName：LoadBalance
 * Package: Client.serviceCenter.balance
 *
 * @ Author：zh
 * @ Create: 2026/1/23 09:17
 * @ Version: 1.0
 * @ Description:负载均衡接口
 */
public interface LoadBalance {
    //负责实现具体算法，返回分配的地址
    String balance(List<String> addressList);
    //添加节点
    void addNode(String node);
    //删除节点
    void delNode(String node);
}
