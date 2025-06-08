// Created: 22.08.2021
package de.freese.jsync.nio.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * NIO Transfer protocol by Frames.<br>
 * See <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md">rSocket-Protocol</a>
 *
 * @author Thomas Freese
 */
public class NioFrameProtocol {
    /**
     * Default: 4 MB
     */
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 4;

    private final ByteBufferPool bufferPool;

    public NioFrameProtocol() {
        this(ByteBufferPool.DEFAULT);
    }

    public NioFrameProtocol(final ByteBufferPool bufferPool) {
        super();

        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool required");
    }

    public ByteBufferPool getBufferPool() {
        return bufferPool;
    }

    /**
     * Read all Frames to the FINISH-Frame.<br>
     * They can be reused by the {@link ByteBufferPool}.
     */
    public Flux<ByteBuffer> readAll(final ReadableByteChannel channel) {
        return Flux.create(sink -> {
            try {
                readAll(channel, sink::next);
            }
            catch (Exception ex) {
                sink.error(ex);
            }
            finally {
                sink.complete();
            }
        });
    }

    /**
     * Read all Frames to the FINISH-Frame.<br>
     * They can be reused by the {@link ByteBufferPool}.
     */
    public void readAll(final ReadableByteChannel channel, final Consumer<ByteBuffer> consumer) throws Exception {
        while (true) {
            final ByteBuffer buffer = readFrame(channel);

            if (buffer == null) {
                // FINISH-Frame
                break;
            }

            consumer.accept(buffer);
        }
    }

    /**
     * The DATA-Frame returns the {@link ByteBuffer} of the Content.<br>
     * ERROR-Frame throws an Exception.<br>
     * FINISH-Frame returns null.
     */
    public ByteBuffer readFrame(final ReadableByteChannel channel) throws Exception {
        // Header
        ByteBuffer buffer = readFrameHeader(channel);

        final FrameType frameType = FrameType.fromEncodedType(buffer.getInt());
        final int contentLength = buffer.getInt();

        getBufferPool().free(buffer);

        // Content
        buffer = getBufferPool().get();

        if (FrameType.DATA.equals(frameType)) {
            read(channel, buffer, contentLength);

            return buffer.flip();
        }
        else if (FrameType.ERROR.equals(frameType)) {
            read(channel, buffer, contentLength);
            buffer.flip();

            final byte[] bytes = new byte[contentLength];
            buffer.get(bytes);
            final String message = new String(bytes, StandardCharsets.UTF_8);

            getBufferPool().free(buffer);

            throw new Exception(message);
        }

        // FINISH-Frame
        getBufferPool().free(buffer);

        return null;
    }

    /**
     * Write the DATA-Frame.
     */
    public void writeData(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
        int contentLength = 0;

        if (buffer.position() == 0) {
            contentLength = buffer.limit();
        }
        else {
            contentLength = buffer.position();
        }

        writeFrameHeader(channel, FrameType.DATA, contentLength);
        write(channel, buffer);
    }

    /**
     * Write the DATA-Frame.
     */
    public void writeData(final WritableByteChannel channel, final Consumer<ByteBuffer> consumer) throws IOException {
        final ByteBuffer buffer = getBufferPool().get();

        try {
            consumer.accept(buffer);

            writeData(channel, buffer);
        }
        finally {
            getBufferPool().free(buffer);
        }
    }

    /**
     * Write the ERROR-Frame.
     */
    public void writeError(final WritableByteChannel channel, final Consumer<ByteBuffer> consumer) throws IOException {
        final ByteBuffer buffer = getBufferPool().get();

        try {
            consumer.accept(buffer);

            int contentLength = 0;

            if (buffer.position() == 0) {
                contentLength = buffer.limit();
            }
            else {
                contentLength = buffer.position();
            }

            writeFrameHeader(channel, FrameType.ERROR, contentLength);
            write(channel, buffer);
        }
        finally {
            getBufferPool().free(buffer);
        }
    }

    /**
     * Write the ERROR-Frame.
     */
    public void writeError(final WritableByteChannel channel, final Throwable th) throws IOException {
        writeError(channel, buffer -> {
            final String message = th.getMessage() == null ? "" : th.getMessage();
            final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            buffer.put(messageBytes);
        });
    }

    /**
     * Write the FINISH-Frame.
     */
    public void writeFinish(final WritableByteChannel channel) throws IOException {
        writeFrameHeader(channel, FrameType.FINISH, 0);
    }

    /**
     * Guarantees that all Data from the Channel are read as expected.
     */
    protected void read(final ReadableByteChannel channel, final ByteBuffer buffer, final int contentLength) throws IOException {
        // The Buffer can be bigger than required.
        final ByteBuffer bb = buffer.slice(0, contentLength);

        int totalRead = channel.read(bb);

        while (totalRead < contentLength) {
            final int bytesRead = channel.read(bb);
            totalRead += bytesRead;
        }

        buffer.position(bb.position());
        buffer.limit(bb.limit());
    }

    protected ByteBuffer readFrameHeader(final ReadableByteChannel channel) throws IOException {
        final ByteBuffer buffer = getBufferPool().get();

        read(channel, buffer, 8);

        return buffer.flip();
    }

    protected long write(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
        // for (ByteBuffer buffer : buffers) {
        // if (buffer.position() > 0)
        // // if (buffer.remaining() != buffer.limit()) {
        // buffer.flip();
        // }
        // }
        //
        // return channel.write(buffers);

        // if (buffer.remaining() != buffer.limit())
        if (buffer.position() > 0) {
            buffer.flip();
        }

        long totalWritten = 0;

        while (buffer.hasRemaining()) {
            final long bytesWritten = channel.write(buffer);

            totalWritten += bytesWritten;
        }

        return totalWritten;
    }

    protected void writeFrameHeader(final WritableByteChannel channel, final FrameType frameType, final int contentLength) throws IOException {
        final ByteBuffer buffer = getBufferPool().get();

        try {
            buffer.putInt(frameType.getEncodedType());
            buffer.putInt(contentLength);

            write(channel, buffer.flip());
        }
        finally {
            getBufferPool().free(buffer);
        }
    }
}
