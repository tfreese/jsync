// Created: 11.01.2017
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * @author Thomas Freese
 */
public class MonitoringReadableByteChannel implements ReadableByteChannel {
    private final LongConsumer bytesReadConsumer;
    private final boolean closeDelegate;
    private final ReadableByteChannel delegate;
    private long bytesRead;

    /**
     * @param bytesReadConsumer {@link BiConsumer}; First Parameter = Number of read Bytes, second Parameter = complete size in Bytes
     * @param size long; complete size in Bytes
     */
    public MonitoringReadableByteChannel(final ReadableByteChannel delegate, final BiConsumer<Long, Long> bytesReadConsumer, final long size, final boolean closeDelegate) {
        this(delegate, br -> bytesReadConsumer.accept(br, size), closeDelegate);
    }

    public MonitoringReadableByteChannel(final ReadableByteChannel delegate, final LongConsumer bytesReadConsumer, final boolean closeDelegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.bytesReadConsumer = Objects.requireNonNull(bytesReadConsumer, "bytesReadConsumer required");
        this.closeDelegate = closeDelegate;
    }

    @Override
    public void close() throws IOException {
        if (this.closeDelegate) {
            this.delegate.close();
        }
    }

    @Override
    public boolean isOpen() {
        return this.delegate.isOpen();
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        final int readCount = this.delegate.read(dst);

        if (readCount > 0) {
            this.bytesRead += readCount;

            this.bytesReadConsumer.accept(this.bytesRead);
        }

        return readCount;
    }
}
