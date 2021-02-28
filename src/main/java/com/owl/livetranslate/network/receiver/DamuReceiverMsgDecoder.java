package com.owl.livetranslate.network.receiver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

@Slf4j
public class DamuReceiverMsgDecoder extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes < ProtocolConsts.HEAD_LENGTH) {
            return;
        }

        //标记一下当前的readIndex的位置
        in.markReaderIndex();

        byte[] headerBytes = new byte[ProtocolConsts.HEAD_LENGTH];
        in.readBytes(headerBytes);
        BiliMsgPacket packet = getHeaderPacketInfo(headerBytes);
        int payloadlength = packet.getPacketlength() - ProtocolConsts.HEAD_LENGTH;
        if (payloadlength < 1) {
            //没有内容了
            return;
        }


        if ((readableBytes - ProtocolConsts.HEAD_LENGTH) < payloadlength) {
            //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex.
            // 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[payloadlength];
        in.readBytes(body);
        if(packet.getVer() != 2 || packet.getAction() != 5){
            dealAndSetBodyInfo(packet, body);
            list.add(packet);
            return;
        }

        Inflater decompress = new Inflater();
        decompress.setInput(body);

        while(!decompress.finished()){
            byte[] decompressHeadBytes = new byte[ProtocolConsts.HEAD_LENGTH];
            decompress.inflate(decompressHeadBytes);
            BiliMsgPacket subPacket = getHeaderPacketInfo(decompressHeadBytes);

            byte[] subBody = new byte[subPacket.getPacketlength() - ProtocolConsts.HEAD_LENGTH];
            decompress.inflate(subBody);

            dealAndSetBodyInfo(subPacket, subBody);
            list.add(subPacket);
        }
        decompress.end();
    }

    private BiliMsgPacket getHeaderPacketInfo(byte[] headerBytes, int offset, int length) {
        ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes, offset, length);
        BiliMsgPacket packet =  BiliMsgPacket.builder()
                .packetlength(headerBuffer.getInt())
                .headerLength(headerBuffer.getShort())
                .ver(headerBuffer.getShort())
                .action(headerBuffer.getInt())
                .param(headerBuffer.getInt()).build();


        if (packet.getPacketlength() < ProtocolConsts.HEAD_LENGTH) {
            log.error("协议失败: (L:" + packet.getPacketlength() + ")");
            throw new RuntimeException("协议失败: (L:" + packet.getPacketlength() + ")");
        }

        return packet;
    }

    private BiliMsgPacket getHeaderPacketInfo(byte[] headerBytes) {
        return getHeaderPacketInfo(headerBytes, 0, headerBytes.length);
    }

    private void dealAndSetBodyInfo(BiliMsgPacket packet, byte[] body) throws JsonProcessingException {
        Map<String, Object> bodyInfo = null;
        switch (packet.getAction()){
            // (OpHeartbeatReply)
            case 3:
                //观众人数
                bodyInfo = new HashMap<>();
                bodyInfo.put("viewer", ByteBuffer.wrap(body).getInt());
                break;
            case 5:
                //playerCommand (OpSendMsgReply)
                String json = new String(body, StandardCharsets.UTF_8);
                log.info("receive json :" + json);
                bodyInfo = new ObjectMapper().readValue(json, Map.class);
                break;
            case 8:
                // (OpAuthReply)
                String str = new String(body, StandardCharsets.UTF_8);
                log.info("receive action 8 :" + str);
                break;
            default:
                log.info("receive other action : " + bodyInfo);
                break;
        }

        packet.setBody(bodyInfo);
    }
}
