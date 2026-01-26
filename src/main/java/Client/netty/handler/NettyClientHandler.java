package Client.netty.handler;

import common.Message.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;

/**
 * ClassName：NettyClientHandler
 * Package: Client.netty.handler
 *
 * @ Author：zh
 * @ Create: 2026/1/14 10:04
 * @ Version: 1.0
 * @ Description:
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final boolean DEBUG_LOG = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        // 接收到response, 给channel设计别名，让sendRequest里读取response
        AttributeKey<CompletableFuture<RpcResponse>> key = AttributeKey.valueOf("RPCResponseFuture");
        CompletableFuture<RpcResponse> future = ctx.channel().attr(key).get();
        if (future != null) {
            future.complete(response);
            ctx.channel().attr(key).set(null);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //异常处理
        if (DEBUG_LOG) {
            cause.printStackTrace();
        }
        AttributeKey<CompletableFuture<RpcResponse>> key = AttributeKey.valueOf("RPCResponseFuture");
        CompletableFuture<RpcResponse> future = ctx.channel().attr(key).get();
        if (future != null) {
            future.completeExceptionally(cause);
            ctx.channel().attr(key).set(null);
        }
        ctx.close();
    }
}
