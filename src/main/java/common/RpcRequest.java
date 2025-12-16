package common;

import lombok.Builder;
import lombok.Data;

/**
 * ClassName：RpcRequest
 * Package: common
 * @ Author：zh
 * @ Create: 2025/12/9 15:18
 * @ Version: 1.0
 * @ Description: 请求信息
 */
@Data
@Builder
public class RpcRequest {
    private  String interfaceName;  //服务类名（客户端只知道接口）

    private String methodName;      //方法名

    private  Object[] params;       //参数

    private Class<?>[] paramTypes;  //参数类型
}
