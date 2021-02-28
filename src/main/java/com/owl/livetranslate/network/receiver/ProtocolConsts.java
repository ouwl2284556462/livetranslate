package com.owl.livetranslate.network.receiver;

public class ProtocolConsts {
    /**
     * 协议头长度
     */
    public static final int HEAD_LENGTH = 16;

    /**
     * 协议规定的magic
     */
    public static final short MAGIC_NUM = 16;

    /**
     * 协议规定的PARAM
     */
    public static final short PARAM = 1;


    /**
     * 加入房间
     */
    public static final int ACTION_JOIN_CHANNEL = 7;

    /**
     * 心跳
     */
    public static final int ACTION_HEARBEAT = 2;
}
