// Created: 07.09.2020
package de.freese.jsync.nio.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.nio.filesystem.RemoteSupport;
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
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().allocate();

        try
        {
            // Response auslesen.
            readResponseHeader(byteBuffer, this.delegate);
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
            ByteBufferPool.getInstance().release(byteBuffer);

            this.channelReleaser.accept(this.delegate);
        }
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
                int bytesWritten = this.delegate.write(src);

                totalWritten += bytesWritten;
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
