// Created: 26.10.2016
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * @author Thomas Freese
 */
public class MonitoringWritableByteChannel implements WritableByteChannel {
    private final LongConsumer bytesWrittenConsumer;
    private final boolean closeDelegate;
    private final WritableByteChannel delegate;
    private long bytesWritten;

    /**
     * @param bytesWrittenConsumer {@link BiConsumer}; First Parameter = Number of read Bytes, second Parameter = complete size in Bytes
     * @param size long; complete size in Bytes
     */
    public MonitoringWritableByteChannel(final WritableByteChannel delegate, final BiConsumer<Long, Long> bytesWrittenConsumer, final long size, final boolean closeDelegate) {
        this(delegate, bw -> bytesWrittenConsumer.accept(bw, size), closeDelegate);
    }

    public MonitoringWritableByteChannel(final WritableByteChannel delegate, final LongConsumer bytesWrittenConsumer, final boolean closeDelegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");
        this.bytesWrittenConsumer = Objects.requireNonNull(bytesWrittenConsumer, "bytesWrittenConsumer required");
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
    public int write(final ByteBuffer src) throws IOException {
        final int writeCount = this.delegate.write(src);

        if (writeCount > 0) {
            this.bytesWritten += writeCount;

            this.bytesWrittenConsumer.accept(this.bytesWritten);
        }

        return writeCount;
    }
}
