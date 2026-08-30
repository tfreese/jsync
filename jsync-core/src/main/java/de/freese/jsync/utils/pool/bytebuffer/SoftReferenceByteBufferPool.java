package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;
import de.freese.jsync.utils.pool.Pool;

/**
 * @author Thomas Freese
 * @since 16.07.2021
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
        return getClass().getSimpleName() + ":"
                + " created=" + getCreated()
                + ", free=" + getFree()
                + ", peak=" + getPeak();
    }

    @Override
    protected ByteBuffer create() {
        return ByteBuffer.allocate(Options.BUFFER_SIZE);
    }
}
