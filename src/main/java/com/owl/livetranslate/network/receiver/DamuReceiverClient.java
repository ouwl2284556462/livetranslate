package com.owl.livetranslate.network.receiver;

import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import com.owl.livetranslate.bean.receiver.ChannelInfo;
import com.owl.livetranslate.utils.RandomUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DamuReceiverClient {

    /**
     * 放松心跳间隔
     */
    private static final int HEARBEAT_INTERVERL_SEC = 3;

    private final ChannelInfo cidInfo;
    private SocketChannel channel;
    private boolean alive;
    private NioEventLoopGroup eventLoopGroup;

    public DamuReceiverClient(ChannelInfo cidInfo) {
        this.cidInfo = cidInfo;
    }

    /**
     * 连接到B站直播服务
     */
    public void connect() throws InterruptedException, UnknownHostException {
        alive = true;

        InetAddress[] allByName = InetAddress.getAllByName(cidInfo.getHost());
        InetAddress ip = allByName[RandomUtils.nextInt(allByName.length)];
        int port = cidInfo.getPort();


        eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.group(eventLoopGroup);
        bootstrap.remoteAddress(ip, port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel){
                socketChannel.pipeline().addLast(new DamuReceiverMsgEncoder());
                socketChannel.pipeline().addLast(new DamuReceiverMsgDecoder());
                socketChannel.pipeline().addLast(new DamuReceiverMsgClientHandler());
            }
        });

        log.info("连接到：ip:{}, port:{}", ip, port);
        ChannelFuture future = bootstrap.connect().sync();
        if(!future.isSuccess()){
            throw new RuntimeException("连接失败");
        }

        channel = (SocketChannel) future.channel();
    }

    /**
     * 加入到房间
     */
    public void joinChannel() throws InterruptedException {
        HashMap<String, Object> body = new HashMap<>();
        body.put("roomid", cidInfo.getRoomId());
        body.put("uid", 0);
        body.put("protover", 2);
        body.put("token", cidInfo.getToken());
        body.put("platform", "danmuji");

        BiliMsgPacket sendPacket = BiliMsgPacket.builder()
                                                .action(ProtocolConsts.ACTION_JOIN_CHANNEL)
                                                .param(ProtocolConsts.PARAM)
                                                .magic(ProtocolConsts.MAGIC_NUM)
                                                .body(body)
                                                .build();

        log.info("joinChannel send:{}", sendPacket);
        if(!channel.writeAndFlush(sendPacket).sync().isSuccess()){
            throw new RuntimeException("加入房间失败。");
        }
    }

    /**
     * 发送心跳
     * @throws InterruptedException
     */
    public void heartbeatLoop() throws InterruptedException {
        BiliMsgPacket sendPacket = BiliMsgPacket.builder()
                                                .action(ProtocolConsts.ACTION_HEARBEAT)
                                                .param(ProtocolConsts.PARAM)
                                                .magic(ProtocolConsts.MAGIC_NUM)
                                                .build();
        while(alive){
            if(!channel.writeAndFlush(sendPacket).sync().isSuccess()){
                log.warn("发送心跳失败");
            }

            TimeUnit.SECONDS.sleep(HEARBEAT_INTERVERL_SEC);
        }
    }

    public synchronized void stop(){
        alive = false;
        if(eventLoopGroup != null){
            if(!eventLoopGroup.isShutdown() && !eventLoopGroup.isShuttingDown()){
                log.info("关闭eventLoopGroup");
                eventLoopGroup.shutdownGracefully();
            }
            eventLoopGroup = null;
        }
    }
}
