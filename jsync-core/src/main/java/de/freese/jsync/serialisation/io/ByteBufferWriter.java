// Created: 22.09.2020
package de.freese.jsync.serialisation.io;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
public class ByteBufferWriter implements DataWriter<ByteBuffer> {
    @Override
    public void writeByte(final ByteBuffer output, final byte value) {
        output.put(value);
    }

    @Override
    public void writeBytes(final ByteBuffer output, final byte[] bytes) {
        output.put(bytes);
    }

    @Override
    public void writeDouble(final ByteBuffer output, final double value) {
        output.putDouble(value);
    }

    @Override
    public void writeFloat(final ByteBuffer output, final float value) {
        output.putFloat(value);
    }

    @Override
    public void writeInteger(final ByteBuffer output, final int value) {
        output.putInt(value);
    }

    @Override
    public void writeLong(final ByteBuffer output, final long value) {
        output.putLong(value);
    }
}
