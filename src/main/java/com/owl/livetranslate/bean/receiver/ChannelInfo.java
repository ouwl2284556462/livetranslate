package com.owl.livetranslate.bean.receiver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelInfo {
    private String token;
    private String host;
    private int port;
}
