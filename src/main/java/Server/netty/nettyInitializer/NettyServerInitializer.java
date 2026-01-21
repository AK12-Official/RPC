package Server.netty.nettyInitializer;

import Server.netty.handler.NettyRPCServerHandler;
import Server.provider.ServiceProvider;
import common.coder.Decoder;
import common.coder.Encoder;
import common.serializer.JsonSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.AllArgsConstructor;


/**
 * ClassName：NettyServerInitializer
 * Package: Server.netty.nettyInitializer
 *
 * @ Author：zh
 * @ Create: 2026/1/14 11:23
 * @ Version: 1.0
 * @ Description:
 */
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private ServiceProvider serviceProvider;
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        //使用自定义的编/解码器
        pipeline.addLast(new Encoder(new JsonSerializer()));
        pipeline.addLast(new Decoder());
        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
