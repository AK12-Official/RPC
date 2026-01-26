package Client.rpcClient.impl;

import Client.netty.nettyInitializer.NettyClientInitializer;
import Client.rpcClient.RpcClient;
import Client.serviceCenter.ServiceCenter;
import Client.serviceCenter.ZKServiceCenter;
import common.Message.RpcRequest;
import common.Message.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * ClassName：NettyRpcClient
 * Package: Client.rpcClient.impl
 *
 * @ Author：zh
 * @ Create: 2026/1/14 09:58
 * @ Version: 1.0
 * @ Description:
 */
public class NettyRpcClient implements RpcClient {

    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private static final ConcurrentHashMap<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>();

    private ServiceCenter serviceCenter;

    public NettyRpcClient(ServiceCenter serviceCenter) throws InterruptedException {
        this.serviceCenter = serviceCenter;
    }

    //netty客户端初始化
    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                //NettyClientInitializer这里 配置netty对消息的处理机制
                .handler(new NettyClientInitializer());
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        //从注册中心获取host post
        InetSocketAddress address = serviceCenter.serviceDiscovery(request.getInterfaceName());
        try {
            return sendWithRetry(address, request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;    //Should not reach here
    }

    private RpcResponse sendWithRetry(InetSocketAddress address, RpcRequest request)
            throws InterruptedException, ExecutionException, ClosedChannelException {
        try {
            return sendOnce(address, request);
        } catch (ClosedChannelException e) {
            Channel cached = CHANNEL_CACHE.get(address);
            if (cached != null) {
                CHANNEL_CACHE.remove(address, cached);
            }
            return sendOnce(address, request);
        }
    }

    private RpcResponse sendOnce(InetSocketAddress address, RpcRequest request)
            throws InterruptedException, ExecutionException, ClosedChannelException {
        Channel channel = getChannel(address);
        if (!channel.isActive()) {
            CHANNEL_CACHE.remove(address, channel);
            channel = getChannel(address);
        }
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        AttributeKey<CompletableFuture<RpcResponse>> key = AttributeKey.valueOf("RPCResponseFuture");
        channel.attr(key).set(responseFuture);
        //发送数据
        channel.writeAndFlush(request).sync();
        RpcResponse response = responseFuture.get();
        System.out.println(response);
        return response;
    }

    private Channel getChannel(InetSocketAddress address) throws InterruptedException {
        final InetSocketAddress cacheKey = address;
        Channel channel = CHANNEL_CACHE.get(cacheKey);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        synchronized (NettyRpcClient.class) {
            channel = CHANNEL_CACHE.get(cacheKey);
            if (channel != null && channel.isActive()) {
                return channel;
            }
            ChannelFuture channelFuture = bootstrap.connect(address).sync();
            channel = channelFuture.channel();
            CHANNEL_CACHE.put(cacheKey, channel);
            final Channel cachedChannel = channel;
            cachedChannel.closeFuture().addListener(future -> CHANNEL_CACHE.remove(cacheKey, cachedChannel));
            return channel;
        }
    }
}
