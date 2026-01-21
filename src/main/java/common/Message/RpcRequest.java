package common.Message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName：RpcRequest
 * Package: common
 * @ Author：zh
 * @ Create: 2025/12/9 15:18
 * @ Version: 1.0
 * @ Description: 请求信息
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RpcRequest implements Serializable {
    private  String interfaceName;  //服务类名（客户端只知道接口） 面向接口编程 使用动态代理，外部给定的信息是接口信息

    private String methodName;      //方法名

    private  Object[] params;       //参数

    private Class<?>[] paramsType;  //参数类型
}
