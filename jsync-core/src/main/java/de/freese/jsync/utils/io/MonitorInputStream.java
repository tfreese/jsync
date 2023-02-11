// Created: 11.01.2017
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

import javax.swing.ProgressMonitorInputStream;

/**
 * @author Thomas Freese
 * @see ProgressMonitorInputStream
 */
public class MonitorInputStream extends InputStream {
    private final LongConsumer bytesReadConsumer;

    private final InputStream delegate;

    private long bytesRead;

    /**
     * @param bytesReadConsumer {@link BiConsumer}; First Parameter = Number of read Bytes, second Parameter = complete size in Bytes
     * @param size long; complete size in Bytes
     */
    public MonitorInputStream(final InputStream delegate, final BiConsumer<Long, Long> bytesReadConsumer, final long size) {
        this(delegate, br -> bytesReadConsumer.accept(br, size));
    }

    public MonitorInputStream(final InputStream delegate, final LongConsumer bytesReadConsumer) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.bytesReadConsumer = Objects.requireNonNull(bytesReadConsumer, "bytesReadConsumer required");
    }

    /**
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return this.delegate.available();
    }

    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        this.delegate.close();
    }

    /**
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public synchronized void mark(final int readLimit) {
        this.delegate.mark(readLimit);
    }

    /**
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return this.delegate.markSupported();
    }

    /**
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        int read = this.delegate.read();

        this.bytesRead++;

        this.bytesReadConsumer.accept(this.bytesRead);

        return read;
    }

    /**
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(final byte[] b) throws IOException {
        int readCount = this.delegate.read(b);

        if (readCount > 0) {
            this.bytesRead += readCount;

            this.bytesReadConsumer.accept(this.bytesRead);
        }

        return readCount;
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int readCount = this.delegate.read(b, off, len);

        if (readCount > 0) {
            this.bytesRead += readCount;

            this.bytesReadConsumer.accept(this.bytesRead);
        }

        return readCount;
    }

    /**
     * @see java.io.InputStream#reset()
     */
    @Override
    public synchronized void reset() throws IOException {
        this.delegate.reset();

        this.bytesRead -= this.delegate.available();

        this.bytesReadConsumer.accept(this.bytesRead);
    }

    /**
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(final long n) throws IOException {
        long readCount = this.delegate.skip(n);

        if (readCount > 0) {
            this.bytesRead += readCount;

            this.bytesReadConsumer.accept(this.bytesRead);
        }

        return readCount;
    }
}
