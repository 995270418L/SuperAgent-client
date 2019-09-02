package com.sa.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
public class ProxyClientHandler extends ChannelInboundHandlerAdapter {

    private ChannelFuture cf;
    private String host;
    private int port;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            log.info("full http request");
            FullHttpRequest request = (FullHttpRequest) msg;
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
            this.host = temp[0];
            this.port = port;
//            ctx.pipeline().remove("httpCodec");
//            ctx.pipeline().remove("httpObject");
            if ("CONNECT".equalsIgnoreCase(request.method().name())) {//HTTPS建立代理握手
                log.info("connect response");
                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                ctx.writeAndFlush(response);
                ctx.pipeline().remove("httpCodec");
                ctx.pipeline().remove("httpObject");
                return;
            }
            log.info("after connect successful");
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop()) // 注册线程池
                    .channel(ctx.channel().getClass())
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            log.info("init channel");
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
                                    log.info("response 1 data :{}", msg);
                                    ctx.channel().writeAndFlush(msg);
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    log.info("channel active");
                                }

                                @Override
                                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                    log.info("channel read complete");
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    log.info("exception occured");
                                }

                            });
                        }
                    })
                    .option(ChannelOption.AUTO_READ, false);

            ChannelFuture cf = bootstrap.connect(this.host, this.port);
            Channel outboundChannel = cf.channel();
            cf.addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
//                        HttpClientCodec codec = new HttpClientCodec();
                        log.info("connect successful 1");
                        ChannelFuture ff = outboundChannel.writeAndFlush(request.content());
                        outboundChannel.read();
                        ff.addListener( fff -> {
                            if(fff.isSuccess()){
                                log.info("write successful");
                            }else{
                                log.info("write failed");
                                log.error("cause : {}", fff.cause());
                            }
                        });
                    } else {
                        log.info("connect failed 1");
                        ctx.channel().close();
                    }
                });
        } else {
            if (cf == null) {
                log.info("channel future is null, build a new one");
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(ctx.channel().eventLoop())
                        .channel(ctx.channel().getClass())
                        .handler(new ChannelInitializer() {

                            @Override
                            protected void initChannel(Channel ch) throws Exception {
                                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
                                        log.info("response data :{}", msg);
                                        ctx.channel().writeAndFlush(msg);
                                    }
                                });
                            }
                        });
                cf = bootstrap.connect(host, port);
                cf.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("connect successful 2");
                            future.channel().writeAndFlush(msg);
                        } else {
                            log.info("connect failed 2");
                            ctx.channel().close();
                        }
                    });
            } else {
                log.info("channel future is not null, close it ");
                cf.channel().writeAndFlush(msg);
            }
        }
    }
}
