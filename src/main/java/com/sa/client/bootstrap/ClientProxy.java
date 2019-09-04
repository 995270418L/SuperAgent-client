package com.sa.client.bootstrap;

import com.sa.client.init.InitChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ClientProxy {

    public static void run(InitChannelInitializer channelInitializer){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // java ServerSocketChannel
                .childHandler(channelInitializer)
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
