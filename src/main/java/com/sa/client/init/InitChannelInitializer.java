package com.sa.client.init;

import com.sa.client.handler.SocksProxyHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InitChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private ApplicationContext ctx;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(ctx.getBean(SocksProxyHandler.class));
    }

}
