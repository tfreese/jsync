// Created: 20.09.2020
package de.freese.jsync.netty.server;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author Thomas Freese
 */
public class JsyncHandler extends ChannelInboundHandlerAdapter
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erstellt ein neues {@link JsyncHandler} Object.
     */
    public JsyncHandler()
    {
        super();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().info("{}: channelActive", ctx.channel().remoteAddress());
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception
    {
        ByteBuf byteBuf = (ByteBuf) msg;

        getLogger().info("{}: channelRead: {}", ctx.channel().remoteAddress(), byteBuf.toString(StandardCharsets.UTF_8));

        ctx.write(msg);
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().info("{}: channelReadComplete", ctx.channel().remoteAddress());

        ctx.flush();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception
    {
        getLogger().error(ctx.channel().remoteAddress().toString(), cause);

        ctx.close();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }
}
