package com.owl.livetranslate.network.receiver;

import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DamuReceiverMsgClientHandler extends SimpleChannelInboundHandler<BiliMsgPacket> {
 
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
        log.info("收到消息执行：" + msg);
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

    }
}