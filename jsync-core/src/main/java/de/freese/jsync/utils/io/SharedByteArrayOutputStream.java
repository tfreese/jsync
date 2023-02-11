// Created: 28.08.2020
package de.freese.jsync.utils.io;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * ByteArrayOutputStream with direkt Access of the ByteArray over a {@link ByteBuffer} without the need to copy it.
 *
 * @author Thomas Freese
 */
public class SharedByteArrayOutputStream extends ByteArrayOutputStream {
    public SharedByteArrayOutputStream() {
        super();
    }

    public SharedByteArrayOutputStream(final int size) {
        super(size);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(this.buf, 0, this.count);
    }

    public void write(final ByteBuffer buffer) {
        write(buffer, buffer.remaining());
    }

    public void write(final ByteBuffer buffer, final int length) {
        byte[] data = new byte[length];
        buffer.get(data);

        writeBytes(data);
    }
}
