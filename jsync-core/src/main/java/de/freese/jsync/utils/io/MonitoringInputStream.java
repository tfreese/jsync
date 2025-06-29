// Created: 11.01.2017
package de.freese.jsync.utils.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * @author Thomas Freese
 */
public class MonitoringInputStream extends FilterInputStream {
    private final LongConsumer bytesReadConsumer;
    private final boolean closeDelegate;

    private long bytesRead;

    /**
     * @param bytesReadConsumer {@link BiConsumer}; 1st Parameter = bytesRead, 2nd Parameter = Size
     */
    public MonitoringInputStream(final InputStream delegate, final BiConsumer<Long, Long> bytesReadConsumer, final long size, final boolean closeDelegate) {
        this(delegate, br -> bytesReadConsumer.accept(br, size), closeDelegate);
    }

    public MonitoringInputStream(final InputStream delegate, final LongConsumer bytesReadConsumer, final boolean closeDelegate) {
        super(delegate);

        this.bytesReadConsumer = Objects.requireNonNull(bytesReadConsumer, "bytesReadConsumer required");
        this.closeDelegate = closeDelegate;
    }

    @Override
    public void close() throws IOException {
        if (closeDelegate) {
            super.close();
        }
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();

        bytesRead++;

        bytesReadConsumer.accept(bytesRead);

        return read;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int readCount = super.read(b, off, len);

        if (readCount > 0) {
            bytesRead += readCount;

            bytesReadConsumer.accept(bytesRead);
        }

        return readCount;
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();

        bytesRead -= available();

        bytesReadConsumer.accept(bytesRead);
    }

    @Override
    public long skip(final long n) throws IOException {
        final long readCount = super.skip(n);

        if (readCount > 0) {
            bytesRead += readCount;

            bytesReadConsumer.accept(bytesRead);
        }

        return readCount;
    }
}
