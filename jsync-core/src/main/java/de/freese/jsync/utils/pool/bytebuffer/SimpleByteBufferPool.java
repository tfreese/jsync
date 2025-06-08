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

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void free(final ByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        free++;

        cache.offer(buffer);
    }

    @Override
    public ByteBuffer get() {
        ByteBuffer buffer = cache.poll();

        if (buffer == null) {
            created++;

            buffer = ByteBuffer.allocate(Options.BUFFER_SIZE);
        }
        else {
            buffer.clear();
        }

        return buffer;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(created);
        sb.append(", free=").append(free);
        sb.append(", size=").append(cache.size());

        return sb.toString();
    }
}
