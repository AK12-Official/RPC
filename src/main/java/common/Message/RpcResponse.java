package common.Message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName：RpcResponse
 * Package: common
 *
 * @ Author：zh
 * @ Create: 2025/12/9 15:49
 * @ Version: 1.0
 * @ Description:返回信息 （类似http格式）
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RpcResponse implements Serializable {
    private int code;       //状态码

    private String message; //状态信息

    private Class<?> dataType;  //传输数据的类型，以便在自定义序列化器中解析

    private Object data;    //具体数据

    //构造成功/失败信息
    public static RpcResponse success(Object data){
        return RpcResponse.builder().code(200).dataType(data.getClass()).data(data).build();
    }
    public static RpcResponse fail(){
        return RpcResponse.builder().code(500).message("服务器发生错误").build();
    }
}
