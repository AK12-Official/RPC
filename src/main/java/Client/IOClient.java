package Client;

import common.Message.RpcRequest;
import common.Message.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * ClassName：IOClient
 * Package: Client
 *
 * @ Author：zh
 * @ Create: 2026/1/13 09:54
 * @ Version: 1.0
 * @ Description:负责底层与服务端的通信，发送request，返回response
 */
@Deprecated
public class IOClient {
    private static final boolean DEBUG_LOG = false;

    public static RpcResponse sendRequest(String host, int port, RpcRequest request) {
        try{
            Socket socket = new Socket(host, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            oos.writeObject(request);
            oos.flush();

            RpcResponse response = (RpcResponse) ois.readObject();
            return response;
        }catch(IOException | ClassNotFoundException e){
            if (DEBUG_LOG) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
