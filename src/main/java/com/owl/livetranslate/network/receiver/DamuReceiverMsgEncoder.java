package com.owl.livetranslate.network.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DamuReceiverMsgEncoder extends MessageToByteEncoder<BiliMsgPacket> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, BiliMsgPacket sendMsgPacket, ByteBuf byteBuf) throws Exception {
        Map<String, Object> body = sendMsgPacket.getBody();
        String bodyStr = "";
        if(body != null && !body.isEmpty()){
            bodyStr = new ObjectMapper().writeValueAsString(body);
        }

        byte[] playload = bodyStr.getBytes(StandardCharsets.UTF_8);
        int packetlength = sendMsgPacket.getPacketlength();
        if (packetlength < 1){
            packetlength = playload.length + 16;
        }


        byteBuf.writeInt(packetlength);
        byteBuf.writeShort(sendMsgPacket.getMagic());
        byteBuf.writeShort(sendMsgPacket.getVer());
        byteBuf.writeInt(sendMsgPacket.getAction());
        byteBuf.writeInt(sendMsgPacket.getParam());

        if (playload.length > 0) {
            byteBuf.writeBytes(playload);
        }
    }
}
