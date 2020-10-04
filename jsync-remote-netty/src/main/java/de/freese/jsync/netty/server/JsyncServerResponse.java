// Created: 30.09.2020
package de.freese.jsync.netty.server;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import de.freese.jsync.remote.RemoteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Thomas Freese
 */
public class JsyncServerResponse
{
    /**
     * @param bufferBody {@link ByteBuf}
     * @return {@link JsyncServerResponse}
     */
    public static JsyncServerResponse error(final ByteBuf bufferBody)
    {
        return new JsyncServerResponse(RemoteUtils.STATUS_ERROR, bufferBody);
    }

    /**
     * @param bufferBody {@link ByteBuf}
     * @return {@link JsyncServerResponse}
     */
    public static JsyncServerResponse ok(final ByteBuf bufferBody)
    {
        return new JsyncServerResponse(RemoteUtils.STATUS_OK, bufferBody);
    }

    /**
     *
     */
    private final ByteBuf bufferBody;

    /**
     *
     */
    private final int status;

    /**
     * Erstellt ein neues {@link JsyncServerResponse} Object.
     *
     * @param status int
     * @param bufferBody {@link ByteBuf}
     */
    private JsyncServerResponse(final int status, final ByteBuf bufferBody)
    {
        super();

        this.status = status;
        this.bufferBody = Objects.requireNonNull(bufferBody, "bufferBody required");
    }

    /**
     * @param ctx {@link ChannelHandlerContext}
     * @throws IOException Falls was schief geht.
     */
    public void write(final ChannelHandlerContext ctx) throws IOException
    {
        this.bufferBody.clear();

        this.bufferBody.writeInt(this.status); // Status
        this.bufferBody.writeLong(0); // Content-Length

        ctx.writeAndFlush(this.bufferBody);
    }

    /**
     * @param ctx {@link ChannelHandlerContext}
     * @param consumer {@link Consumer}
     * @throws IOException Falls was schief geht.
     */
    public void write(final ChannelHandlerContext ctx, final Consumer<ByteBuf> consumer) throws IOException
    {
        this.bufferBody.clear();

        consumer.accept(this.bufferBody);

        ByteBuf bufferHeader = ctx.alloc().directBuffer(12);
        bufferHeader.writeInt(this.status); // Status
        bufferHeader.writeLong(this.bufferBody.writerIndex()); // Content-Length

        ctx.write(bufferHeader.retain());
        ctx.writeAndFlush(this.bufferBody.retain());

        // Damit der Buffer wieder in den Pool kommt ohne IllegalReferenceCountException.
        // bufferHeader.retain();
        // bufferHeader.release();
    }
}
