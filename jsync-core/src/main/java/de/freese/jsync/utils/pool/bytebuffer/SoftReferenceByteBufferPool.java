// Created: 16.07.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;
import de.freese.jsync.utils.pool.Pool;

/**
 * @author Thomas Freese
 */
class SoftReferenceByteBufferPool extends Pool<ByteBuffer> implements ByteBufferPool {
    SoftReferenceByteBufferPool() {
        super(true, true);
    }

    @Override
    public ByteBuffer get() {
        return obtain();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(getCreated());
        sb.append(", free=").append(getFree());
        sb.append(", peak=").append(getPeak());

        return sb.toString();
    }

    @Override
    protected ByteBuffer create() {
        return ByteBuffer.allocate(Options.BUFFER_SIZE);
    }
}
