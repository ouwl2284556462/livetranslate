package com.owl.livetranslate;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.livetranslate.bean.receiver.BiliMsgPacket;
import com.owl.livetranslate.bean.receiver.ChannelInfo;
import com.owl.livetranslate.network.receiver.DamuReceiver;
import com.owl.livetranslate.network.receiver.DamuReceiverMsgClientHandler;
import com.owl.livetranslate.network.receiver.DamuReceiverMsgDecoder;
import com.owl.livetranslate.network.receiver.DamuReceiverMsgEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class LivetranslateApplicationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private DamuReceiver damuReceiver;

	//@Autowired
	//private DamuSender damuSender;


	//@Test
	void testGetChannelId(){
		//test
		int roomId = 21449083;
		ChannelInfo cidInfo = damuReceiver.getCidInfo(roomId);

		System.out.println(cidInfo.getToken());
		System.out.println(cidInfo.getHost());
		System.out.println(cidInfo.getPort());
	}



	//@Test
	void testListenToRoom() throws InterruptedException, UnknownHostException {
		damuReceiver.startListenToRoom(21547895,
				logMsg ->{
					System.out.println(logMsg);
				},
				client ->{
					System.out.println(client);
				},
				danmuInfo -> {

				},
				ctx ->{

				});

		while(true){

		}
	}


	
//	void test1() throws InterruptedException {
//		String cookied = "_uuid=BB2375DC-CE77-8175-C672-F8300002F07452279infoc; buvid3=63B55CE7-4588-4AAF-BA42-31A88CF423B6138401infoc; rpdid=|(u))lkl~Yu|0J'ulmlmJJmmJ; blackside_state=1; LIVE_BUVID=AUTO2915987986197584; CURRENT_FNVAL=80; CURRENT_QUALITY=112; bp_video_offset_13676195=479380245013295485; bp_t_offset_13676195=479384582930307455; buvid_fp=63B55CE7-4588-4AAF-BA42-31A88CF423B6138401infoc; sid=4fxmt7el; _dfcaptcha=4caa94be3dd453dbaa3c7789551ba9f9; fingerprint=ced2b88220c366d132b465da905aa25e; buvid_fp_plain=63B55CE7-4588-4AAF-BA42-31A88CF423B6138401infoc; DedeUserID=13676195; DedeUserID__ckMd5=1e06e45efd69224b; SESSDATA=e27b390a,1627886487,e2c15*21; bili_jct=bf8faf6b5faa3827bfdf77c9f74a1a5b; PVID=18";
//		String csrfByCookied = damuSender.getCsrfByCookied(cookied);
//		damuSender.sendDamu(7595278, "6", cookied, csrfByCookied);
//		TimeUnit.SECONDS.sleep(1);
//		damuSender.sendDamu(870004, "6", cookied, csrfByCookied);
//	}


}
