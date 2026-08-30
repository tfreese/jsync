package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 * @since 23.08.2021
 */
class NoByteBufferPool implements ByteBufferPool {
    private int created;
    private int free;

    NoByteBufferPool() {
        super();
    }

    @Override
    public void clear() {
        // Empty
    }

    @Override
    public void free(final ByteBuffer buffer) {
        free++;
    }

    @Override
    public ByteBuffer get() {
        created++;

        return ByteBuffer.allocate(Options.BUFFER_SIZE);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":"
                + " created=" + created
                + ", free=" + free;
    }
}
