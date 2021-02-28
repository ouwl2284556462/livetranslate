package com.owl.livetranslate.network.sender;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

@Component
@Slf4j
public class DamuSender {

    @Value("${damu.url}")
    private String url;


    private String formatWithName = "【%s:%s】";


    private String formatNoName = "【%s】";



    @Autowired
    private RestTemplate restTemplate;


    public String getCsrfByCookied(String cookied) {
        String jctKey = "bili_jct=";
        String[] cookiedPairs = cookied.split("; ");
        for (String cookiedPair : cookiedPairs) {
            if (cookiedPair.startsWith("bili_jct=")) {
                return cookiedPair.substring(jctKey.length());
            }
        }

        return null;
    }

    public void sendDamu(int roomid, String msg, String cookied, String speaker){
        sendDamu(roomid, msg, getCsrfByCookied(cookied), speaker);
    }


    public void sendDamu(int roomid, String msg, String cookied, String csrf, String speaker) {
        if(StringUtils.hasText(speaker)){
            msg = String.format(formatWithName, speaker, msg);
        }else{
            msg = String.format(formatNoName, msg);
        }


        HttpHeaders headers = new HttpHeaders();
        headers.put(HttpHeaders.COOKIE, Arrays.asList(cookied));
        headers.put(HttpHeaders.ACCEPT_ENCODING, Arrays.asList("gzip, deflate, br"));
        headers.put(HttpHeaders.USER_AGENT, Arrays.asList("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.104 Safari/537.36"));
        headers.put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList("zh-CN,zh;q=0.9"));
        headers.put(HttpHeaders.CONTENT_TYPE, Arrays.asList("application/x-www-form-urlencoded; charset=UTF-8"));


        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("msg", msg);
        params.add("color", 16777215);
        params.add("fontsize", 25);
        params.add("mode", 1);
        params.add("rnd", LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8")));
        params.add("roomid", roomid);
        params.add("bubble", 0);
        params.add("csrf_token", csrf);
        params.add("csrf", csrf);


        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);
        log.info(request.toString());
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info(response.toString());
    }

}
