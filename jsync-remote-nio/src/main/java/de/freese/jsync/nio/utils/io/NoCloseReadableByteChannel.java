// Created: 07.09.2020
package de.freese.jsync.nio.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Thomas Freese
 */
public class NoCloseReadableByteChannel implements ReadableByteChannel
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
     * Erstellt ein neues {@link NoCloseReadableByteChannel} Object.
     *
     * @param delegate {@link SocketChannel}
     * @param channelReleaser {@link Consumer}
     */
    public NoCloseReadableByteChannel(final SocketChannel delegate, final Consumer<SocketChannel> channelReleaser)
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
        this.channelReleaser.accept(this.delegate);
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
     * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
     */
    @Override
    public int read(final ByteBuffer dst) throws IOException
    {
        try
        {
            return this.delegate.read(dst);
        }
        catch (IOException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            IOException e = new IOException(ex.getMessage(), ex.getCause());
            e.setStackTrace(ex.getStackTrace());

            throw e;
        }
    }
}
