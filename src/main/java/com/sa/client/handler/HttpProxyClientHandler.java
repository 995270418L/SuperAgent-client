package com.sa.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HttpProxyClientHandler extends ChannelInboundHandlerAdapter {

    private final Channel clientChannel;

    private final Object msg;

    public HttpProxyClientHandler(Channel clientChannel, Object msg) {
        this.clientChannel = clientChannel;
        this.msg = msg;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel active");
        ctx.writeAndFlush(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            log.info("change http response");
            FullHttpResponse response = (FullHttpResponse) msg;
            response.headers().add("X-test", "for-test");
            clientChannel.writeAndFlush(msg);
        }finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
