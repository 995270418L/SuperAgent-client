package com.sa.client.handler;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpProxyInitializer extends ChannelInitializer {

    private final Channel clientChannel;
    private final Object msg;

    public HttpProxyInitializer(Channel relayChannel, Object msg) {
        this.clientChannel = relayChannel;
        this.msg = msg;
    }

    @Override
    protected void initChannel(Channel ch) {
        log.info("init channel");
        ChannelPipeline p = ch.pipeline();
//        p.addLast("httpCodec", new HttpServerCodec());
//        p.addLast("httpObject", new HttpObjectAggregator(6553600));
        p.addLast(new HttpProxyClientHandler(clientChannel, msg));
    }

}
