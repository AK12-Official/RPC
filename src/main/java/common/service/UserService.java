package common.service;

import common.pojo.User;

/**
 * ClassName：UserService
 * Package: common.service
 *
 * @ Author：zh
 * @ Create: 2026/1/13 09:50
 * @ Version: 1.0
 * @ Description:
 */
public interface UserService {
    // 客户端通过这个接口调用服务端的实现类
    User getUserByUserId(Integer id);
    //新增一个功能
    Integer insertUserId(User user);
}