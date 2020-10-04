// Created: 20.09.2020
package de.freese.jsync.netty.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Thomas Freese
 */
public class JsyncNettyServerInitializer extends ChannelInitializer<SocketChannel>
{
    /**
     * Erstellt ein neues {@link JsyncNettyServerInitializer} Object.
     */
    public JsyncNettyServerInitializer()
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
        pipeline.addLast(new JsyncNettyHandler());

        // ch.config().setAllocator(allocator)
    }
}
