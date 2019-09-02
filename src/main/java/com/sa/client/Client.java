package com.sa.client;

import com.sa.client.handler.ClientHandler;
import com.sa.client.handler.HttpProxyWriteHandler;
import com.sa.client.handler.ProxyClient2Handler;
import com.sa.client.handler.ProxyClientHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Client {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        ServerBootstrap bootstrap = new ServerBootstrap();
        SelfSignedCertificate ssc = new SelfSignedCertificate();
//        SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // java ServerSocketChannel
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
//                        p.addLast(sslContext.newHandler(ch.alloc()));
//                        p.addLast("httpCodec",new HttpServerCodec());
//                        p.addLast("httpObject", new HttpObjectAggregator(65536));
//                        p.addLast(new ProxyClientHandler());
                        p.addLast(new ClientHandler());
//                        p.addLast(new HttpProxyWriteHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1024) // 连接数
                .childOption(ChannelOption.SO_KEEPALIVE, false);

        try {
            ChannelFuture f = bootstrap.bind(1080).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

}
