package com.dgcheshang.cheji.nettygps.init;

import android.util.Log;

import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.nettygps.conf.Params;
import com.dgcheshang.cheji.nettygps.task.GpsConTask;

import java.util.Timer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class GpsClient implements Runnable{

    private static Bootstrap b;

    public void run(){
        if(NettyConf.debug) {
            Log.e("TAG", "启动连接！");
        }
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new GpsClientInitializer());
            //自动调整下一次缓冲区建立时分配的空间大小，避免内存的浪费
            b.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
            //使用内存池
            b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,30*1000);
            // 连接服务端

            if(NettyConf.debug) {
                Log.e("TAG", Params.gpshost + ":" + Params.gpsport);
            }

            ChannelFuture f = b.connect(Params.gpshost, Params.gpsport).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch(Exception e){
            new Timer().schedule(new GpsConTask(),30*1000);
        }finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
    }
}

