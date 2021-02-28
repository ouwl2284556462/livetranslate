package com.owl.livetranslate.network.receiver;

import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import com.owl.livetranslate.bean.receiver.DanmuInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class DamuReceiverMsgClientHandler extends SimpleChannelInboundHandler<BiliMsgPacket> {

    private final Consumer<DanmuInfo> danmuCb;

    public DamuReceiverMsgClientHandler(Consumer<DanmuInfo> danmuCb) {
        this.danmuCb = danmuCb;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        log.info("断开连接执行");
    }
 
    @Override
    public void channelActive(ChannelHandlerContext ctx){
        log.info("连接成功执行");
    }

 
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("抛出异常执行", cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BiliMsgPacket msg)  {
        log.debug("收到消息执行：" + msg);
        if(msg.getAction() != 5){
            return;
        }

        Map<String, Object> body = msg.getBody();
        if(body == null){
            return;
        }

        String cmd = (String) body.get("cmd");
        if("DANMU_MSG".equals(cmd)){
            dealDanmuMsg(body);
        }
    }

    private void dealDanmuMsg(Map<String, Object> body) {
        List<Object> infoList = (List<Object>) body.get("info");
        List<Object>  userInfo = (List<Object>) infoList.get(2);
        DanmuInfo danmuInfo = DanmuInfo.builder()
                                        .content((String) infoList.get(1))
                                        .uid((int)userInfo.get(0))
                                        .uname((String)userInfo.get(1))
                                        .build();

        log.debug("收到弹幕信息：{}", danmuInfo);
        danmuCb.accept(danmuInfo);
    }
}