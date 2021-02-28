package com.owl.livetranslate.bean.receiver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DanmuInfo {
    private String content;
    private String uname;
    private int uid;
}
