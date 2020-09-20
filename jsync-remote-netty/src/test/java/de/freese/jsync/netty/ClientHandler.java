// Created: 20.09.2020
package de.freese.jsync.netty;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;

/**
 * @author Thomas Freese
 */
public class ClientHandler extends ChannelInboundHandlerAdapter // SimpleChannelInboundHandler<String> // ChannelInboundHandlerAdapter
{
    /**
     *
     */
    private ChannelHandlerContext ctx;

    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     *
     */
    private final BlockingQueue<Promise<String>> messageList = new ArrayBlockingQueue<>(16);

    /**
     * Erstellt ein neues {@link ClientHandler} Object.
     */
    public ClientHandler()
    {
        super();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().info("{}: channelActive", ctx.channel().localAddress());

        this.ctx = ctx;

        // ByteBuf byteBuf = Unpooled.buffer(128);
        // byteBuf.writeBytes("Hello".getBytes(StandardCharsets.UTF_8));
        // ctx.writeAndFlush(byteBuf);
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().info("{}: channelInactive", ctx.channel().localAddress());

        super.channelInactive(ctx);

        synchronized (this)
        {
            Promise<String> prom;

            while ((prom = this.messageList.poll()) != null)
            {
                prom.setFailure(new IOException("Connection lost"));
            }
        }
    }

    // /**
    // * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
    // */
    // @Override
    // protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception
    // {
    // getLogger().info("{}: channelRead0", ctx.channel().localAddress());
    //
    // // ctx.write(msg);
    //
    // // if (this.messageList.isEmpty())
    // // {
    // // return;
    // // }
    //
    // synchronized (this)
    // {
    // if ((this.messageList != null) && !this.messageList.isEmpty())
    // {
    // this.messageList.poll().setSuccess(msg);
    // }
    // }
    // }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception
    {
        getLogger().info("{}: channelRead", ctx.channel().localAddress());

        ByteBuf byteBuf = (ByteBuf) msg;

        synchronized (this)
        {
            if ((this.messageList != null) && !this.messageList.isEmpty())
            {
                this.messageList.poll().setSuccess(byteBuf.toString(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().info("{}: channelReadComplete", ctx.channel().localAddress());

        ctx.flush();
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception
    {
        getLogger().error(ctx.channel().localAddress().toString(), cause);

        ctx.close();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param message String
     * @return {@link Future}
     */
    public Future<String> sendMessage(final String message)
    {
        if (this.ctx == null)
        {
            throw new IllegalStateException();
        }

        return sendMessage(message, this.ctx.executor().newPromise());
    }

    /**
     * @param message String
     * @param promise {@link Promise}
     * @return {@link Future}
     */
    private Future<String> sendMessage(final String message, final Promise<String> promise)
    {
        synchronized (this)
        {
            if (this.messageList == null)
            {
                // Connection closed
                promise.setFailure(new IllegalStateException());
            }
            else if (this.messageList.offer(promise))
            {
                // Connection open and message accepted
                // this.ctx.writeAndFlush(message);

                ByteBuf byteBuf = Unpooled.buffer(128);
                byteBuf.writeBytes(message.getBytes(StandardCharsets.UTF_8));

                this.ctx.writeAndFlush(byteBuf);// .addListener();
            }
            else
            {
                // Connection open and message rejected
                promise.setFailure(new BufferOverflowException());
            }

            return promise;
        }
    }
}
