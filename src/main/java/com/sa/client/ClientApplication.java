package com.sa.client;

import com.sa.client.bootstrap.ClientProxy;
import com.sa.client.init.InitChannelInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ClientApplication {

    private static InitChannelInitializer channelInitializer;

    public ClientApplication(InitChannelInitializer _channelInitializer) {
        this.channelInitializer = _channelInitializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
        ClientProxy.run(channelInitializer);
    }

}
