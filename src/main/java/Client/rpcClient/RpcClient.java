package Client.rpcClient;

import common.Message.RpcRequest;
import common.Message.RpcResponse;

/**
 * ClassName：RpcClient
 * Package: Client.rpcClient
 *
 * @ Author：zh
 * @ Create: 2026/1/14 09:55
 * @ Version: 1.0
 * @ Description:
 */
public interface RpcClient {

    //定义底层通信的方法
    RpcResponse sendRequest(RpcRequest request);
}
