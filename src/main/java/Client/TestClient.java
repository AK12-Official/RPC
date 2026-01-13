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
    public static void main(String[] args) {
        //创建ClientProxy对象
        ClientProxy clientProxy = new ClientProxy("127.0.0.1", 19999);
        //通过ClientProxy对象获取代理对象
        UserService proxy = clientProxy.getProxy(UserService.class);
        //调用代理对象的方法
        User user = proxy.getUserByUserId(1);
        System.out.println("User Got From Server: " + user);

        User u=User.builder().id(100).userName("lzx").sex(true).build();
        Integer id =proxy.insertUserId(u);
        System.out.println("The Id Inserted to Server: " + id);
    }
}
