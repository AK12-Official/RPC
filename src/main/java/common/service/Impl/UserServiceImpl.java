package common.service.Impl;

import common.pojo.User;
import common.service.UserService;

import java.util.Random;
import java.util.UUID;

/**
 * ClassName：UserServiceImpl
 * Package: common.service.Impl
 *
 * @ Author：zh
 * @ Create: 2026/1/13 09:51
 * @ Version: 1.0
 * @ Description:
 */
public class UserServiceImpl implements UserService {
    private static final boolean DEBUG_LOG = false;

    @Override
    public User getUserByUserId(Integer id) {
        if (DEBUG_LOG) {
            System.out.println("客户端查询了" + id + "的用户");
        }
        // 模拟从数据库中取用户的行为
        Random random = new Random();
        User user = User.builder().userName(UUID.randomUUID().toString())
                .id(id)
                .sex(random.nextBoolean()).build();
        return user;
    }

    @Override
    public Integer insertUserId(User user) {
        if (DEBUG_LOG) {
            System.out.println("插入数据成功" + user.getUserName());
        }
        return user.getId();
    }
}
