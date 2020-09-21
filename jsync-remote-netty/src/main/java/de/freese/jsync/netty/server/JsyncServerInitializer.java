// Created: 20.09.2020
package de.freese.jsync.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Thomas Freese
 */
public class JsyncServerInitializer extends ChannelInitializer<SocketChannel>
{
    /**
     * Erstellt ein neues {@link JsyncServerInitializer} Object.
     */
    public JsyncServerInitializer()
    {
        super();
    }

    /**
     * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
     */
    @Override
    protected void initChannel(final SocketChannel ch) throws Exception
    {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new JsyncHandler());
    }
}