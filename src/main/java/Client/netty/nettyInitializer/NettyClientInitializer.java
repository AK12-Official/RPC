package Client.netty.nettyInitializer;

import Client.netty.handler.NettyClientHandler;
import common.coder.Decoder;
import common.coder.Encoder;
import common.serializer.JsonSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;


/**
 * ClassName：NettyClientInitializer
 * Package: Client.netty.nettyInitializer
 *
 * @ Author：zh
 * @ Create: 2026/1/14 10:04
 * @ Version: 1.0
 * @ Description:
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //使用自定义的编/解码器
        pipeline.addLast(new Decoder());
        pipeline.addLast(new Encoder(new JsonSerializer()));
        pipeline.addLast(new NettyClientHandler());
    }
}
