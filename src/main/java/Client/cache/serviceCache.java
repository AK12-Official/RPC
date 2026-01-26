package Client.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClassName：serviceCache
 * Package: cache
 *
 * @ Author：zh
 * @ Create: 2026/1/22 13:12
 * @ Version: 1.0
 * @ Description:客户端本地缓存——存储服务信息
 */
public class serviceCache {
    private static final boolean DEBUG_LOG = false;

    //key: serviceName 服务名
    //value： addressList 服务提供者列表
    private static final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    // 白名单（允许重试的服务名集合）
    private static final Set<String> retryWhitelist = ConcurrentHashMap.newKeySet();

    //添加服务
    public void addServcieToCache(String serviceName,String address){
        cache.compute(serviceName, (k, v) -> {
            List<String> addressList = (v == null) ? new CopyOnWriteArrayList<>() : v;
            addressList.add(address);
            return addressList;
        });
        if (DEBUG_LOG) {
            System.out.println("将name为" + serviceName + "和地址为" + address + "的服务添加到本地缓存中");
        }
    }

    //修改服务地址
    public void replaceServiceAddress(String serviceName,String oldAddress,String newAddress){
        if(cache.containsKey(serviceName)){
            List<String> addressList=cache.get(serviceName);
            addressList.remove(oldAddress);
            addressList.add(newAddress);
        }else {
            if (DEBUG_LOG) {
                System.out.println("修改失败，服务不存在");
            }
        }
    }

    //从缓存中取服务地址
    public  List<String> getServcieFromCache(String serviceName){
        if(!cache.containsKey(serviceName)) {
            return null;
        }
        List<String> a=cache.get(serviceName);
        return a;
    }

    //从缓存中删除服务地址
    public void delete(String serviceName,String address){
        List<String> addressList = cache.get(serviceName);
        if (addressList != null) {
            addressList.remove(address);
            if (DEBUG_LOG) {
                System.out.println("将name为" + serviceName + "和地址为" + address + "的服务从本地缓存中删除");
            }
        }
    }

    // ===== 重试白名单缓存 =====
    public boolean isInRetryWhitelist(String serviceName) {
        return retryWhitelist.contains(serviceName);
    }

    public void addRetryServiceToCache(String serviceName) {
        retryWhitelist.add(serviceName);
    }

    public void removeRetryServiceFromCache(String serviceName) {
        retryWhitelist.remove(serviceName);
    }

    public void replaceRetryWhitelist(List<String> serviceNames) {
        retryWhitelist.clear();
        if (serviceNames == null) {
            return;
        }
        retryWhitelist.addAll(serviceNames);
    }
}
