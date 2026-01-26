package Client.serviceCenter.balance.Impl;

import Client.serviceCenter.balance.LoadBalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ClassName：RandomLoadBalance
 * Package: Client.serviceCenter.balance
 *
 * @ Author：zh
 * @ Create: 2026/1/23 09:18
 * @ Version: 1.0
 * @ Description:随机算法负载均衡
 */
public class RandomLoadBalance implements LoadBalance {
    private static final boolean DEBUG_LOG = false;

    @Override
    public String balance(List<String> addressList) {
        if (addressList == null || addressList.isEmpty()) {
            throw new IllegalArgumentException("addressList is empty");
        }
        int choose = ThreadLocalRandom.current().nextInt(addressList.size());
        if (DEBUG_LOG) {
            System.out.println("负载均衡选择了" + choose + "服务器");
        }
        return addressList.get(choose);
    }

    @Override
    public void addNode(String node) {}

    @Override
    public void delNode(String node) {}
}
