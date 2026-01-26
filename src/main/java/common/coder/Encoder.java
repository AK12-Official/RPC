package common.coder;

import common.Message.MessageType;
import common.Message.RpcRequest;
import common.Message.RpcResponse;
import common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

/**
 * ClassName：MyEncoder
 * Package: common.serializer
 *
 * @ Author：zh
 * @ Create: 2026/1/21 23:55
 * @ Version: 1.0
 * @ Description:自定义编码器,传入的数据为request或者response
 *    持有一个serialize器，负责将传入的对象序列化成字节数组
 */
@AllArgsConstructor
public class Encoder extends MessageToByteEncoder {
    private Serializer serializer;

    private static final boolean DEBUG_LOG = false;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (DEBUG_LOG) {
            System.out.println(msg.getClass());
        }
        //写入消息类型
        if(msg instanceof RpcRequest){
            out.writeShort(MessageType.REQUEST.getCode());
        } else if(msg instanceof RpcResponse){
            out.writeShort(MessageType.RESPONSE.getCode());
        }
        //2.写入序列化方式
        out.writeShort(serializer.getType());
        //得到序列化数组
        byte[] serializeBytes = serializer.serialize(msg);
        //3.写入长度
        out.writeInt(serializeBytes.length);
        //4.写入序列化数组
        out.writeBytes(serializeBytes);
    }
}
