package com.dgcheshang.cheji.nettygps.init;

import com.dgcheshang.cheji.netty.conf.NettyConf;
import com.dgcheshang.cheji.netty.util.ByteUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

public class GpsClientInitializer extends ChannelInitializer<SocketChannel> {
    private final static int readerIdleTimeSeconds = 90;//读操作空闲30秒
    private final static int allIdleTimeSeconds = 90;//读写全部空闲100秒
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        /*
         * 这个地方的 必须和服务端对应上。否则无法正常解码和编码
         *
         * 解码和编码 我将会在下一张为大家详细的讲解。再次暂时不做详细的描述
         *
         * */
        ByteBuf delimiter = Unpooled.copiedBuffer(ByteUtil.hexStringToByte("7E"));
        //pipeline.addLast("framer", new DelimiterBasedFrameDecoder(81920, Delimiters.lineDelimiter()));
        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(1024, delimiter));
        //pipeline.addLast("decoder", new GpsDecoder());
        pipeline.addLast("encoder", new GpsEncoder());
        pipeline.addLast("idleStateHandler", new IdleStateHandler(readerIdleTimeSeconds,NettyConf.xtjg,allIdleTimeSeconds));

        // 客户端的逻辑
        pipeline.addLast("handler", new GpsClientHandler());
    }

}