package com.owl.livetranslate.network.receiver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import com.owl.livetranslate.bean.receiver.ChannelInfo;
import com.owl.livetranslate.bean.receiver.DanmuInfo;
import com.owl.livetranslate.utils.RandomUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class DamuReceiver {

    @Value("${damu.receive.cidInfoUrl}")
    private String cidInfoUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExecutorService executorService;

    public ChannelInfo getCidInfo(int roomid){
        String targetUrl = cidInfoUrl + roomid;
        String responseStr = restTemplate.getForObject(targetUrl, String.class);
        log.info(responseStr);
        Map<String, Object> responseMap;
        try {
            responseMap = new ObjectMapper().readValue(responseStr, Map.class);
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            String token =  (String) data.get("token");
            String host =  (String) data.get("host");
            int port = (int) data.get("port");
            return ChannelInfo.builder().host(host)
                                        .token(token)
                                        .port(port)
                                        .roomId(roomid)
                                        .build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("解析json错误：", e);
            return null;
        }
    }

    /**
     * 开始监听房间信息
     * @param roomId
     */
    public void startListenToRoom(int roomId, Consumer<String> logInfo, Consumer<DamuReceiverClient> startSuccessCb, Consumer<DanmuInfo> danmuCb, Consumer<ChannelHandlerContext> disconnectCb) {
        executorService.execute(() ->{
            logInfo.accept("获取房间信息中...");
            ChannelInfo cidInfo = getCidInfo(roomId);
            if(null == cidInfo){
                log.error("获取房间失败，roomid:{}", roomId);
                logInfo.accept("获取房间失败，roomid:" + roomId);
                return;
            }

            DamuReceiverClient client = null;
            try{
                logInfo.accept("连接服务器中...");
                client = new DamuReceiverClient(cidInfo);
                client.connect(danmuCb, disconnectCb);
                log.info("连接成功");
                logInfo.accept("连接成功");

                logInfo.accept("进入房间中...");
                client.joinChannel();
                log.info("进入房间成功");
                logInfo.accept("进入房间成功");

                startSuccessCb.accept(client);
                client.heartbeatLoop();
            }catch (Exception e){
                log.error("接收失败", e);
                logInfo.accept("接收失败：" + e.getMessage());
            }finally {
                if(client != null){
                    client.stop();
                }
            }
        });
    }
}
