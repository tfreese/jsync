// Created: 20.09.2020
package de.freese.jsync.netty.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.netty.model.adapter.ByteBufAdapter;
import de.freese.jsync.netty.server.JsyncServerResponse;
import de.freese.jsync.remote.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Thomas Freese
 */
public class JsyncNettyHandler extends SimpleChannelInboundHandler<ByteBuf> // ChannelInboundHandlerAdapter
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncNettyHandler.class);

    /**
     *
     */
    private static final ThreadLocal<Receiver> THREAD_LOCAL_RECEIVER = ThreadLocal.withInitial(LocalhostReceiver::new);

    /**
    *
    */
    private static final ThreadLocal<Sender> THREAD_LOCAL_SENDER = ThreadLocal.withInitial(LocalhostSender::new);

    /**
     *
     */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link JsyncNettyHandler} Object.
     */
    public JsyncNettyHandler()
    {
        super();
    }

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
        getLogger().debug("{}: channelRead0: {}/{}", ctx.channel().remoteAddress(), buf, buf.hashCode());

        JSyncCommand command = getSerializer().readFrom(buf, JSyncCommand.class);
        getLogger().debug("{}: read command: {}", ctx.channel().remoteAddress(), command);

        if (command == null)
        {
            getLogger().error("unknown JSyncCommand");
            return;
        }

        switch (command)
        {
            case DISCONNECT:
                JsyncServerResponse.ok(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, "DISCONNECTED"));

                ctx.disconnect();
                ctx.close();
                break;

            case CONNECT:
                JsyncServerResponse.ok(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, "CONNECTED"));
                break;

            case READ_CHUNK:
                readChunk(ctx, buf, THREAD_LOCAL_SENDER.get());
                break;

            case SOURCE_CHECKSUM:
                createChecksum(ctx, buf, THREAD_LOCAL_SENDER.get());
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                createSyncItems(ctx, buf, THREAD_LOCAL_SENDER.get());
                break;
        }

        // Damit der Buffer wieder in den Pool kommt ohne IllegalReferenceCountException.
        // buf.retain();
        // buf.release();
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
     * Create the checksum.
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param buf {@link ByteBuf}
     * @param fileSystem {@link FileSystem}
     */
    protected void createChecksum(final ChannelHandlerContext ctx, final ByteBuf buf, final FileSystem fileSystem)
    {
        String baseDir = getSerializer().readFrom(buf, String.class);
        String relativeFile = getSerializer().readFrom(buf, String.class);

        Exception exception = null;
        String checksum = null;

        try
        {
            checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
            });
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        if (exception != null)
        {
            getLogger().error(null, exception);

            try
            {
                // Exception senden.
                Exception ex = exception;
                JsyncServerResponse.error(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, ex, Exception.class));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            try
            {
                // Response senden.
                String chksm = checksum;
                JsyncServerResponse.ok(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, chksm));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
    }

    /**
     * Create the Sync-Items.
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param buf {@link ByteBuf}
     * @param fileSystem {@link FileSystem}
     */
    protected void createSyncItems(final ChannelHandlerContext ctx, final ByteBuf buf, final FileSystem fileSystem)
    {
        String baseDir = getSerializer().readFrom(buf, String.class);
        boolean followSymLinks = getSerializer().readFrom(buf, Boolean.class);

        Exception exception = null;
        List<SyncItem> syncItems = new ArrayList<>(128);

        try
        {
            fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
                getLogger().debug("{}: SyncItem generated: {}", ctx.channel().remoteAddress(), syncItem);

                syncItems.add(syncItem);
            });
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        if (exception != null)
        {
            getLogger().error(null, exception);

            try
            {
                // Exception senden.
                Exception ex = exception;
                JsyncServerResponse.error(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, ex, Exception.class));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            try
            {
                // Response senden.
                JsyncServerResponse.ok(buf).write(ctx, buffer -> {
                    buffer.writeInt(syncItems.size());

                    for (SyncItem syncItem : syncItems)
                    {
                        getSerializer().writeTo(buf, syncItem);
                    }
                });
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
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
        return LOGGER;
    }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    protected Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

    /**
     * Read File-Chunk from Sender.
     *
     * @param ctx {@link ChannelHandlerContext}
     * @param buf {@link ByteBuf}
     * @param sender {@link Sender}
     */
    protected void readChunk(final ChannelHandlerContext ctx, final ByteBuf buf, final Sender sender)
    {
        String baseDir = getSerializer().readFrom(buf, String.class);
        String relativeFile = getSerializer().readFrom(buf, String.class);
        long position = buf.readLong();
        long sizeOfChunk = buf.readLong();

        Exception exception = null;
        ByteBuffer bufferChunk = ByteBufferPool.getInstance().get();

        try
        {
            sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, bufferChunk);
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        if (exception != null)
        {
            getLogger().error(null, exception);

            try
            {
                // Exception senden.
                Exception ex = exception;
                JsyncServerResponse.error(buf).write(ctx, buffer -> getSerializer().writeTo(buffer, ex, Exception.class));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            // Response senden
            // JsyncServerResponse.ok(buffer).write(selectionKey);
            buf.clear();
            buf.writeInt(RemoteUtils.STATUS_OK); // Status
            buf.writeLong(sizeOfChunk); // Content-Length

            bufferChunk.flip();
            buf.writeBytes(bufferChunk);

            ctx.writeAndFlush(buf.retain());
        }

        ByteBufferPool.getInstance().release(bufferChunk);
    }
}
