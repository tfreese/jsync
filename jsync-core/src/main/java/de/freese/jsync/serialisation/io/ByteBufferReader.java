// Created: 22.09.2020
package de.freese.jsync.serialisation.io;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
public class ByteBufferReader implements DataReader<ByteBuffer> {
    @Override
    public byte readByte(final ByteBuffer input) {
        return input.get();
    }

    @Override
    public byte[] readBytes(final ByteBuffer input, final int length) {
        final byte[] bytes = new byte[length];
        input.get(bytes);

        return bytes;
    }

    @Override
    public double readDouble(final ByteBuffer input) {
        return input.getDouble();
    }

    @Override
    public float readFloat(final ByteBuffer input) {
        return input.getFloat();
    }

    @Override
    public int readInteger(final ByteBuffer input) {
        return input.getInt();
    }

    @Override
    public long readLong(final ByteBuffer input) {
        return input.getLong();
    }
}
