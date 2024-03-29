package com.owl.livetranslate;

import com.owl.livetranslate.ui.LivetranslateFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
public class LivetranslateApplication{
	
    public static void main(String[] args) throws URISyntaxException, IOException {
       // SpringApplicationBuilder builder = new SpringApplicationBuilder(LivetranslateApplication.class);
       // ConfigurableApplicationContext applicationContext = builder.headless(false).web(WebApplicationType.NONE).run(args);
    	System.setProperty("java.awt.headless","false");
        ConfigurableApplicationContext applicationContext = SpringApplication.run(LivetranslateApplication.class, args);
        //new LivetranslateFrame(applicationContext);
    }

}
