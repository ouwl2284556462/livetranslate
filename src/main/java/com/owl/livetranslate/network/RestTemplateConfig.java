package com.owl.livetranslate.network;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 添加内容转换器,使用默认的内容转换器
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
        // 设置编码格式为UTF-8
        List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();
        for (int i = 0; i < converterList.size(); i++) {
            if (converterList.get(i).getClass() == StringHttpMessageConverter.class) {
                // 设置编码格式为UTF-8
                converterList.set(i, new StringHttpMessageConverter(StandardCharsets.UTF_8));
                break;
            }
        }

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    @Bean
    public HttpClient httpClient() {
        // 长连接保持30秒
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
        //设置整个连接池最大连接数 根据自己的场景决定
        connectionManager.setMaxTotal(50);
        //同路由的并发数,路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(50);

        //requestConfig
        RequestConfig requestConfig = RequestConfig.custom()
                                    //服务器返回数据(response)的时间，超过该时间抛出read timeout
                                    .setSocketTimeout(10000)
                                    //连接上服务器(握手成功)的时间，超出该时间抛出connect timeout
                                    .setConnectTimeout(5000)
                                    //从连接池中获取连接的超时时间，超过该时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException: Timeout waiting for connection from pool
                                    .setConnectionRequestTimeout(500)
                                    .build();


        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                // 保持长连接配置，需要在头添加Keep-Alive
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                //重试次数，默认是3次，没有开启
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }
}
