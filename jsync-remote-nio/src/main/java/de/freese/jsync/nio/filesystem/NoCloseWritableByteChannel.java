// Created: 07.09.2020
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class NoCloseWritableByteChannel implements WritableByteChannel, RemoteSupport
{

    /**
      *
      */
    private final Consumer<SocketChannel> channelReleaser;

    /**
    *
    */
    private final SocketChannel delegate;

    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Erstellt ein neues {@link NoCloseWritableByteChannel} Object.
     *
     * @param delegate {@link NetworkChannel}
     * @param channelReleaser {@link Consumer}
     */
    public NoCloseWritableByteChannel(final SocketChannel delegate, final Consumer<SocketChannel> channelReleaser)
    {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.channelReleaser = Objects.requireNonNull(channelReleaser, "channelReleaser required");
    }

    /**
     * @see java.nio.channels.Channel#close()
     */
    @Override
    public void close() throws IOException
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();
        buffer.clear();

        try
        {
            // Response auslesen.
            readResponseHeader(this.delegate);
        }
        catch (IOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            IOException ioex = new IOException(ex.getMessage(), ex.getCause());
            ioex.setStackTrace(ex.getStackTrace());

            throw ioex;
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
            this.channelReleaser.accept(this.delegate);
        }
    }

    /**
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see java.nio.channels.Channel#isOpen()
     */
    @Override
    public boolean isOpen()
    {
        return this.delegate.isOpen();
    }

    /**
     * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
     */
    @Override
    public int write(final ByteBuffer src) throws IOException
    {
        try
        {
            int totalWritten = 0;

            while (src.hasRemaining())
            {
                totalWritten += this.delegate.write(src);
            }

            return totalWritten;
        }
        catch (IOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            IOException ioex = new IOException(ex.getMessage(), ex.getCause());
            ioex.setStackTrace(ex.getStackTrace());

            throw ioex;
        }
    }
}
