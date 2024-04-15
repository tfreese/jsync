// Created: 04.10.2020
package de.freese.jsync.rsocket.serialisation;

import io.netty.buffer.ByteBuf;

import de.freese.jsync.serialisation.io.DataReader;

/**
 * @author Thomas Freese
 */
public class ByteBufReader implements DataReader<ByteBuf> {
    @Override
    public byte readByte(final ByteBuf input) {
        return input.readByte();
    }

    @Override
    public byte[] readBytes(final ByteBuf input, final int length) {
        final byte[] bytes = new byte[length];
        input.readBytes(bytes);

        return bytes;
    }

    @Override
    public double readDouble(final ByteBuf input) {
        return input.readDouble();
    }

    @Override
    public float readFloat(final ByteBuf input) {
        return input.readFloat();
    }

    @Override
    public int readInteger(final ByteBuf input) {
        return input.readInt();
    }

    @Override
    public long readLong(final ByteBuf input) {
        return input.readLong();
    }
}
