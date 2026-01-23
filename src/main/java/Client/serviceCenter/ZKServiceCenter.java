package Client.serviceCenter;

import Client.cache.serviceCache;
import Client.serviceCenter.ZkWatcher.watchZK;
import Client.serviceCenter.balance.Impl.ConsistencyHashBalance;
import Client.serviceCenter.balance.LoadBalance;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * ClassName：ZKServiceCenter
 * Package: Client.serviceCenter
 *
 * @ Author：zh
 * @ Create: 2026/1/16 13:32
 * @ Version: 1.0
 * @ Description:
 */
public class ZKServiceCenter implements ServiceCenter{
    // curator 提供的zookeeper客户端
    private CuratorFramework client;
    //zookeeper根路径节点
    private static final String ROOT_PATH = "MyRPC";
    private static final String RETRY = "CanRetry";
    //服务缓存
    private serviceCache cache;

    private LoadBalance loadBalance;

    //负责zookeeper客户端的初始化 并与zookeeper服务端进行连接
    public ZKServiceCenter() throws InterruptedException {
        //指数时间重试
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);
        //zookeeper地址固定 不管是服务提供者还是消费者都要与之连接
        //sessionTimeoutMs 与zoo.cfg中的tickTime有关系
        //zk还会根据minSessionTimeout与maxSessionTimeout两个参数重新调整最后的超时值 默认分别为tickTime的2倍和20倍
        // 使用心跳监听状态
        this.client = CuratorFrameworkFactory.builder().connectString("1.92.112.182:2181")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");
        //初始化本地缓存
        cache=new serviceCache();
        //加入zookeeper事件监听器
        watchZK watcher=new watchZK(client,cache);
        //监听启动
        watcher.watchToUpdate(ROOT_PATH);
        //初始化负载均衡器 默认使用一致性哈希算法
        this.loadBalance=new ConsistencyHashBalance();
    }

    public ZKServiceCenter(LoadBalance loadBalance) throws InterruptedException {
        this();
        this.loadBalance = loadBalance;
    }

    //根据服务名返回地址
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        try {
            //先从本地缓存中找
            List<String> serviceList=cache.getServcieFromCache(serviceName);
            //如果找不到，再去zookeeper中找
            //这种i情况基本不会发生，或者说只会出现在初始化阶段
            if(serviceList==null) {
                serviceList=client.getChildren().forPath("/" + serviceName);
            }
            // 负载均衡得到地址
            String address = loadBalance.balance(serviceList);
            return parseAddress(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 地址 -> XXX.XXX.XXX.XXX:port 字符串
    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" +
                serverAddress.getPort();
    }
    // 字符串解析为地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }

    @Override
    public boolean checkRetry(String serviceName) {
        boolean canRetry =false;
        try {
            List<String> serviceList = client.getChildren().forPath("/" + RETRY);
            for(String s:serviceList){
                //如果列表中有该服务
                if(s.equals(serviceName)){
                    System.out.println("服务"+serviceName+"在白名单上，可进行重试");
                    canRetry=true;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return canRetry;
    }
}
