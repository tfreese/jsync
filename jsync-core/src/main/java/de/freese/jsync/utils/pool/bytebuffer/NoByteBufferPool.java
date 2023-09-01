// Created: 23.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;

/**
 * @author Thomas Freese
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
        this.free++;
    }

    @Override
    public ByteBuffer get() {
        this.created++;

        return ByteBuffer.allocate(Options.BUFFER_SIZE);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(this.created);
        sb.append(", free=").append(this.free);

        return sb.toString();
    }
}
