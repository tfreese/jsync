// Created: 04.10.2020
package de.freese.jsync.rsocket.serialisation;

import io.netty.buffer.ByteBuf;

import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public class ByteBufWriter implements DataWriter<ByteBuf> {
    @Override
    public void writeByte(final ByteBuf output, final byte value) {
        output.writeByte(value);
    }

    @Override
    public void writeBytes(final ByteBuf output, final byte[] bytes) {
        output.writeBytes(bytes);
    }

    @Override
    public void writeDouble(final ByteBuf output, final double value) {
        output.writeDouble(value);
    }

    @Override
    public void writeFloat(final ByteBuf output, final float value) {
        output.writeFloat(value);
    }

    @Override
    public void writeInteger(final ByteBuf output, final int value) {
        output.writeInt(value);
    }

    @Override
    public void writeLong(final ByteBuf output, final long value) {
        output.writeLong(value);
    }
}
