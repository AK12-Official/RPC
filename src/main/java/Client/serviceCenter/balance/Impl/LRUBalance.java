package Client.serviceCenter.balance.Impl;

import Client.serviceCenter.balance.LoadBalance;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClassName：LRUBalance
 * Package: Client.serviceCenter.balance.Impl
 *
 * @ Author：zh
 * @ Create: 2026/1/26 09:40
 * @ Version: 1.0
 * @ Description:LRU算法负载均衡
 */
public class LRUBalance implements LoadBalance {
    private final Object lock = new Object();

    /**
     * accessOrder=true：每次 get/put 都会把条目移动到末尾（最近使用）。
     * 这样 entrySet().iterator().next() 就是“最久未使用”的节点。
     */
    private final LinkedHashMap<String, String> lruMap = new LinkedHashMap<>(16, 0.75f, true);

    @Override
    public String balance(List<String> addressList) {
        if (addressList == null || addressList.isEmpty()) {
            throw new IllegalArgumentException("addressList is empty");
        }

        synchronized (lock) {
            // 1) 对齐：剔除已下线节点
            Set<String> alive = new HashSet<>(addressList);
            lruMap.keySet().removeIf(addr -> !alive.contains(addr));

            // 2) 对齐：加入新节点（新节点当作“最近使用”，避免一上来就被挑中导致集中）
            for (String addr : addressList) {
                if (!lruMap.containsKey(addr)) {
                    lruMap.put(addr, addr);
                }
            }

            // 3) 选择：最久未使用（map 头部）
            Map.Entry<String, String> eldest = lruMap.entrySet().iterator().next();
            String chosen = eldest.getKey();

            // 4) 标记为“最近使用”（移动到尾部）
            lruMap.get(chosen);

            System.out.println("LRU负载均衡选择了服务器：" + chosen);
            return chosen;
        }
    }

    @Override
    public void addNode(String node) {
        if (node == null || node.isEmpty()) {
            return;
        }
        synchronized (lock) {
            lruMap.putIfAbsent(node, node);
        }
    }

    @Override
    public void delNode(String node) {
        if (node == null || node.isEmpty()) {
            return;
        }
        synchronized (lock) {
            lruMap.remove(node);
        }
    }
}
