// Created: 20.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import de.freese.jsync.Options;

/**
 * SimpleByteBufferPool: created=9, free=951: 2192x1: 8176x1: 18235x1: 25538x1: 1242454x1: 1990756x1: 4194304x3
 *
 * @author Thomas Freese
 */
class SimpleByteBufferPool implements ByteBufferPool {
    private final Queue<ByteBuffer> cache = new LinkedBlockingQueue<>(Integer.MAX_VALUE);

    private int created;

    private int free;

    SimpleByteBufferPool() {
        super();
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#clear()
     */
    @Override
    public void clear() {
        this.cache.clear();
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#free(java.nio.ByteBuffer)
     */
    @Override
    public void free(final ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        this.free++;

        this.cache.offer(buffer);
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#get()
     */
    @Override
    public ByteBuffer get() {
        ByteBuffer buffer = this.cache.poll();

        if (buffer == null) {
            this.created++;

            buffer = ByteBuffer.allocate(Options.BUFFER_SIZE);
        }
        else {
            buffer.clear();
        }

        return buffer;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(this.created);
        sb.append(", free=").append(this.free);
        sb.append(", size=").append(this.cache.size());

        return sb.toString();
    }
}
