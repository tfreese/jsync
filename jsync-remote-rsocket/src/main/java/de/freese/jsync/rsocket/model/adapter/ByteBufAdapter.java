// Created: 04.10.2020
package de.freese.jsync.rsocket.model.adapter;

import io.netty.buffer.ByteBuf;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public class ByteBufAdapter implements DataAdapter<ByteBuf, ByteBuf> {
    @Override
    public byte readByte(final ByteBuf source) {
        return source.readByte();
    }

    @Override
    public byte[] readBytes(final ByteBuf source, final int length) {
        byte[] bytes = new byte[length];
        source.readBytes(bytes);

        return bytes;
    }

    @Override
    public double readDouble(final ByteBuf source) {
        return source.readDouble();
    }

    @Override
    public float readFloat(final ByteBuf source) {
        return source.readFloat();
    }

    @Override
    public int readInteger(final ByteBuf source) {
        return source.readInt();
    }

    @Override
    public long readLong(final ByteBuf source) {
        return source.readLong();
    }

    @Override
    public void writeByte(final ByteBuf sink, final byte value) {
        sink.writeByte(value);
    }

    @Override
    public void writeBytes(final ByteBuf sink, final byte[] bytes) {
        sink.writeBytes(bytes);
    }

    @Override
    public void writeDouble(final ByteBuf sink, final double value) {
        sink.writeDouble(value);
    }

    @Override
    public void writeFloat(final ByteBuf sink, final float value) {
        sink.writeFloat(value);
    }

    @Override
    public void writeInteger(final ByteBuf sink, final int value) {
        sink.writeInt(value);
    }

    @Override
    public void writeLong(final ByteBuf sink, final long value) {
        sink.writeLong(value);
    }
}
