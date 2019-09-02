package com.sa.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyClient2Handler extends ChannelInboundHandlerAdapter {

    private ChannelFuture cf;
    private String host;
    private int port;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String host = request.headers().get("host");
            String[] temp = host.split(":");
            int port = 80;
            if (temp.length > 1) {
                port = Integer.parseInt(temp[1]);
            } else {
                if (request.uri().indexOf("https") == 0) {
                    port = 443;
                }
            }
            this.port = port;
            this.host = temp[0];

            final Channel inboundChannel = ctx.channel();
            // Start the connection attempt.
            Bootstrap b = new Bootstrap();
            b.group(inboundChannel.eventLoop())
                    .channel(ctx.channel().getClass())
                    .handler(new ChannelInitializer(){

                        @Override
                        protected void initChannel(Channel ch) throws Exception {

                            log.info("channel initialize");
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    log.info("channel active");
                                    ctx.read();
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    log.info("channel read");
                                    inboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener)future -> {
                                        if(future.isSuccess()){
                                            log.info("channel read write successful");
                                            ctx.channel().read();
                                        }else{
                                            log.info("channel read write failed");
                                            future.channel().close();
                                        }
                                    });
                                }

                            });
                        }

                    })
                    .option(ChannelOption.AUTO_READ, false);
            ChannelFuture f = b.connect(this.host, this.port);
            Channel outboundChannel = f.channel();
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        // connection complete start to read first data
                        inboundChannel.read();
                        log.info("write ready");
                        outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture ff) {
                                if (ff.isSuccess()) {
                                    log.info("write successful");
                                    // was able to flush out data, start to read the next chunk
                                    inboundChannel.read();
                                } else {
                                    log.info("write failed");
                                    ff.channel().close();
                                }
                            }
                        });
                    } else {
                        // Close the connection if the connection attempt has failed.
                        inboundChannel.close();
                    }
                }
            });
        }
    }
}
