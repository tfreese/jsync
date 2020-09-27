// Created: 20.09.2020
package de.freese.jsync.netty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;

/**
 * @author Thomas Freese
 */
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> // ChannelInboundHandlerAdapter
{
    /**
     *
     */
    private final PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

    /**
     *
     */
    private ChannelHandlerContext ctx;

    // /**
    // *
    // */
    // private final BlockingQueue<Promise<String>> messageList = new ArrayBlockingQueue<>(16);

    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
    *
    */
    private final Map<CharSequence, Promise<String>> requests = new ConcurrentHashMap<>();

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
        getLogger().debug("{}: channelActive", ctx.channel().localAddress());

        this.ctx = ctx;
    }

    // /**
    // * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
    // */
    // @Override
    // public void channeleRead(final ChannelHandlerContext ctx, final Object msg) throws Exception
    // {
    // getLogger().info("{}: channelRead", ctx.channel().localAddress());
    //
    // ByteBuf byteBuf = (ByteBuf) msg;
    //
    // synchronized (this)
    // {
    // if ((this.messageList != null) && !this.messageList.isEmpty())
    // {
    // this.messageList.poll().setSuccess(byteBuf.toString(StandardCharsets.UTF_8));
    // }
    // }
    // }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().debug("{}: channelInactive", ctx.channel().localAddress());

        super.channelInactive(ctx);

        Set<CharSequence> requestIds = this.requests.keySet();

        for (CharSequence requestId : requestIds)
        {
            Promise<String> promise = this.requests.remove(requestId);
            promise.setFailure(new IOException("Connection lost"));
        }
        // synchronized (this)
        // {
        // Promise<String> promise;
        //
        // while ((promise = this.messageList.poll()) != null)
        // {
        // promise.setFailure(new IOException("Connection lost"));
        // }
        // }
    }

    /**
     * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf buf) throws Exception
    {
        int requestIdLength = buf.readInt();
        CharSequence requestId = buf.readCharSequence(requestIdLength, StandardCharsets.UTF_8);

        int messageLength = buf.readInt();
        CharSequence message = buf.readCharSequence(messageLength, StandardCharsets.UTF_8);

        getLogger().info("{}: channelRead0; {}/{}", ctx.channel().localAddress(), requestId, message);

        Promise<String> promise = this.requests.remove(requestId);
        promise.setSuccess(message.toString());

        // synchronized (this)
        // {
        // if ((this.messageList != null) && !this.messageList.isEmpty())
        // {
        // this.messageList.poll().setSuccess(msg.toString(StandardCharsets.UTF_8));
        // }
        // }
    }

    /**
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception
    {
        getLogger().debug("{}: channelReadComplete", ctx.channel().localAddress());

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
        UUID uuid = UUID.randomUUID();
        String requestId = uuid.toString();
        // String requestId = org.apache.commons.lang3.RandomStringUtils.randomNumeric(16);

        ByteBuf buf = this.allocator.buffer();
        getLogger().info("Buffer: {}/{}", buf, buf.hashCode());

        buf.clear();
        buf.writeInt(requestId.length());
        buf.writeCharSequence(requestId, StandardCharsets.UTF_8);
        buf.writeInt(message.length());
        buf.writeCharSequence(message, StandardCharsets.UTF_8);

        this.requests.put(requestId, promise);

        this.ctx.writeAndFlush(buf);

        return promise;

        // synchronized (this)
        // {
        // if (this.messageList == null)
        // {
        // // Connection closed
        // promise.setFailure(new IllegalStateException());
        // }
        // else if (this.messageList.offer(promise))
        // {
        // // Connection open and message accepted
        // // this.ctx.writeAndFlush(message);
        //
        // ByteBuf buf = this.allocator.buffer();
        // getLogger().info("Buffer: {}/{}", buf, buf.hashCode());
        //
        // buf.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        //
        // this.ctx.writeAndFlush(buf);// .addListener();
        // }
        // else
        // {
        // // Connection open and message rejected
        // promise.setFailure(new BufferOverflowException());
        // }
        //
        // return promise;
        // }
    }
}
