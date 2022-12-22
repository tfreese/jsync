// Created: 11.01.2017
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * @author Thomas Freese
 */
public class MonitorOutputStream extends OutputStream
{
    private final LongConsumer bytesWrittenConsumer;
    private final OutputStream delegate;
    private long bytesWritten;

    /**
     * @param bytesWrittenConsumer {@link BiConsumer}; First Parameter = Number of read Bytes, second Parameter = complete size in Bytes
     * @param size long; complete size in Bytes
     */
    public MonitorOutputStream(final OutputStream delegate, final BiConsumer<Long, Long> bytesWrittenConsumer, final long size)
    {
        this(delegate, bw -> bytesWrittenConsumer.accept(bw, size));
    }

    public MonitorOutputStream(final OutputStream delegate, final LongConsumer bytesWrittenConsumer)
    {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.bytesWrittenConsumer = Objects.requireNonNull(bytesWrittenConsumer, "bytesWrittenConsumer required");
    }

    /**
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        this.delegate.close();
    }

    /**
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException
    {
        this.delegate.flush();
    }

    /**
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(final byte[] b) throws IOException
    {
        this.delegate.write(b);

        this.bytesWritten += b.length;

        this.bytesWrittenConsumer.accept(this.bytesWritten);
    }

    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException
    {
        this.delegate.write(b, off, len);

        this.bytesWritten += len;

        this.bytesWrittenConsumer.accept(this.bytesWritten);
    }

    /**
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(final int b) throws IOException
    {
        this.delegate.write(b);

        this.bytesWritten++;

        this.bytesWrittenConsumer.accept(this.bytesWritten);
    }
}
