package com.owl.livetranslate.network.receiver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.ChannelInfo;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class DamuReceiver {

    @Value("${damu.receive.cidInfoUrl}")
    private String cidInfoUrl;

    @Autowired
    private RestTemplate restTemplate;

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
            return ChannelInfo.builder().host(host).token(token).port(port).build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("解析json错误：", e);
            return null;
        }
    }
}
