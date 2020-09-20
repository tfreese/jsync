// Created: 20.09.2020
package de.freese.jsync.netty.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Thomas Freese
 */
public class JsyncHandler2 extends SimpleChannelInboundHandler<ByteBuf>
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erstellt ein neues {@link JsyncHandler2} Object.
     */
    public JsyncHandler2()
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
     * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception
    {
        getLogger().info("{}: channelRead0: {}", ctx.channel().remoteAddress(), msg);

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
        getLogger().error(null, cause);

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
