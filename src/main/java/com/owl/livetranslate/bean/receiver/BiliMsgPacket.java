package com.owl.livetranslate.bean.receiver;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class BiliMsgPacket {
    private int packetlength;
    private short magic;
    private short ver;
    private short headerLength;
    private int action;
    private int param;
    private Map<String, Object> body;
}
