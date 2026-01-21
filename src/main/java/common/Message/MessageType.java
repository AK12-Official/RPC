package common.Message;

import lombok.AllArgsConstructor;

/**
 * ClassName：MessageType
 * Package: common.Message
 *
 * @ Author：zh
 * @ Create: 2026/1/22 00:27
 * @ Version: 1.0
 * @ Description:
 */
@AllArgsConstructor
public enum MessageType {
    REQUEST(0),RESPONSE(1);
    private int code;
    public int getCode(){
        return code;
    }
}
