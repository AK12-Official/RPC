package Client;

import Client.proxy.ClientProxy;
import common.pojo.User;
import common.service.UserService;

/**
 * ClassName：TestClient
 * Package: Client
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:19
 * @ Version: 1.0
 * @ Description:
 */
public class TestClient {
    public static void main(String[] args) throws InterruptedException {
        // 压测参数：默认 1000 次，可通过命令行传入：java ... TestClient 5000
        int requestCount = 10000;
        if (args != null && args.length > 0) {
            try {
                requestCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        // 创建 ClientProxy（不再需要像旧版本一样写死 ip & port）
        ClientProxy clientProxy = new ClientProxy();
        UserService proxy = clientProxy.getProxy(UserService.class);

        // 预热：避免首次连接/类加载/序列化初始化对统计造成影响
        int warmupCount = Math.min(20, requestCount);
        for (int i = 0; i < warmupCount; i++) {
            proxy.getUserByUserId(1);
        }

        // 正式统计：连续发起 requestCount 次请求
        long startNs = System.nanoTime();
        User lastUser = null;
        for (int i = 0; i < requestCount; i++) {
            // 这里选择一个典型的 RPC 调用作为压测目标
            lastUser = proxy.getUserByUserId(1);
        }
        long endNs = System.nanoTime();

        long totalNs = endNs - startNs;
        double totalMs = totalNs / 1_000_000.0;
        double avgMs = totalMs / requestCount;

        System.out.println("PerfTest finished.");
        System.out.println("Requests: " + requestCount);
        System.out.println(String.format("Total: %.3f ms", totalMs));
        System.out.println(String.format("Avg:   %.6f ms/req", avgMs));
        System.out.println("Last response (sample): " + lastUser);
    }
}
