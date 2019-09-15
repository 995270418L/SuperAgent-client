package com.sa.client.handler;

import com.sa.client.http.HttpMiddleware;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Scope("prototype")
public class HttpProxyHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    private HttpMiddleware httpMiddleware;
    private Channel remoteChannel;

    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        flushAndClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error("shit happens: {}", e);
        flushAndClose(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(httpMiddleware.isComplete()){
            log.info("already read complete");
            remoteChannel.writeAndFlush(msg);
            return;
        }

        httpMiddleware.digest(msg);
        log.info("http middle ware: {}", httpMiddleware.toString());

        if(httpMiddleware.isHttps()){
            log.info("https connect requests received");
            String res ="HTTP/1.1 200 OK" + System.lineSeparator() + System.lineSeparator();
            ctx.writeAndFlush(Unpooled.wrappedBuffer(res.getBytes()));
        }
        // http or others
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
                                log.info("response data: {}", msg);
                                ctx.channel().writeAndFlush(msg);
                            }
                        });
                    }
                });
        ChannelFuture cf = bootstrap.connect(httpMiddleware.getHost(), httpMiddleware.getPort());
        remoteChannel = cf.channel();
        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("future success");
                if(!httpMiddleware.isHttps()) {
                    remoteChannel.writeAndFlush(msg);
                }else{
                    // 非 https 就释放 msg
                    ReferenceCountUtil.release(msg);
                }
            } else {
                log.info("future failed");
                ctx.channel().close();
            }
        });
    }

    private void flushAndClose(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
