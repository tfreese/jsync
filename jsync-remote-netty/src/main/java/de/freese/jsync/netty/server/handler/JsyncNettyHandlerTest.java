// Created: 20.09.2020
package de.freese.jsync.netty.server.handler;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Thomas Freese
 */
public class JsyncNettyHandlerTest extends SimpleChannelInboundHandler<ByteBuf> // ChannelInboundHandlerAdapter
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().debug("{}: channelActive", ctx.channel().remoteAddress());
    }

    // /**
    // * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
    // */
    // @Override
    // public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception
    // {
    // ByteBuf byteBuf = (ByteBuf) msg;
    // String text = byteBuf.toString(StandardCharsets.UTF_8);
    //
    // getLogger().info("{}: channelRead: {}", ctx.channel().remoteAddress(), text);
    //
    // byteBuf.clear();
    // byteBuf.writeCharSequence(text + ", from Server", StandardCharsets.UTF_8);
    // ctx.write(msg);
    // }

    /**
     * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf buf) throws Exception
    {
        getLogger().info("{}: channelRead0: {}/{}", ctx.channel().remoteAddress(), buf, buf.hashCode());

        int requestIdLength = buf.readInt();
        CharSequence requestId = buf.readCharSequence(requestIdLength, StandardCharsets.UTF_8);

        int messageLength = buf.readInt();
        CharSequence message = buf.readCharSequence(messageLength, StandardCharsets.UTF_8);

        getLogger().info("{}: channelRead0: {}/{}", ctx.channel().remoteAddress(), requestId, message);

        String response = message + ", from Server";

        buf.clear();
        buf.writeInt(requestId.length());
        buf.writeCharSequence(requestId, StandardCharsets.UTF_8);
        buf.writeInt(response.length());
        buf.writeCharSequence(response, StandardCharsets.UTF_8);

        ctx.write(buf.retain());
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().debug("{}: channelReadComplete", ctx.channel().remoteAddress());

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
