// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter.impl;

import java.nio.ByteBuffer;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public class ByteBufferAdapter implements DataAdapter<ByteBuffer, ByteBuffer> {
    @Override
    public byte readByte(final ByteBuffer source) {
        return source.get();
    }

    @Override
    public byte[] readBytes(final ByteBuffer source, final int length) {
        byte[] bytes = new byte[length];
        source.get(bytes);

        return bytes;
    }

    @Override
    public double readDouble(final ByteBuffer source) {
        return source.getDouble();
    }

    @Override
    public float readFloat(final ByteBuffer source) {
        return source.getFloat();
    }

    @Override
    public int readInteger(final ByteBuffer source) {
        return source.getInt();
    }

    @Override
    public long readLong(final ByteBuffer source) {
        return source.getLong();
    }

    @Override
    public void writeByte(final ByteBuffer sink, final byte value) {
        sink.put(value);
    }

    @Override
    public void writeBytes(final ByteBuffer sink, final byte[] bytes) {
        sink.put(bytes);
    }

    @Override
    public void writeDouble(final ByteBuffer sink, final double value) {
        sink.putDouble(value);
    }

    @Override
    public void writeFloat(final ByteBuffer sink, final float value) {
        sink.putFloat(value);
    }

    @Override
    public void writeInteger(final ByteBuffer sink, final int value) {
        sink.putInt(value);
    }

    @Override
    public void writeLong(final ByteBuffer sink, final long value) {
        sink.putLong(value);
    }
}
