package Server.provider;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName：ServiceProvider
 * Package: Server.provider
 *
 * @ Author：zh
 * @ Create: 2026/1/13 10:31
 * @ Version: 1.0
 * @ Description: 本地服务存放器
 */
public class ServiceProvider {
    //集合中存放服务的实例
    private Map<String, Object> interfaceProvider;

    public ServiceProvider() {
        this.interfaceProvider = new HashMap<>();
    }

    //本地注册服务
    public void provideServiceInterface(Object service) {
        String  serviceName=service.getClass().getName();
        Class<?>[] interfaceName = service.getClass().getInterfaces();

        for(Class<?> clazz:interfaceName){
            interfaceProvider.put(clazz.getName(),service); //接口名 对象
        }
    }

    //获取服务实例
    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }
}
