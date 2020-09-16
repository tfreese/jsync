// Created: 07.09.2020
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Thomas Freese
 * @param <T> Type
 */
public class NoCloseReadableByteChannel<T extends NetworkChannel> implements ReadableByteChannel
{
    /**
     *
     */
    private final ChannelReader channelReader;

    /**
    *
    */
    private final Consumer<T> channelReleaser;

    /**
    *
    */
    private final T delegate;

    /**
     * Erstellt ein neues {@link NoCloseReadableByteChannel} Object.
     *
     * @param delegate {@link NetworkChannel}
     * @param channelReader {@link ChannelReader}
     * @param channelReleaser {@link Consumer}
     */
    public NoCloseReadableByteChannel(final T delegate, final ChannelReader channelReader, final Consumer<T> channelReleaser)
    {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.channelReader = Objects.requireNonNull(channelReader, "channelReader required");
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
            return this.channelReader.read(dst);
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
