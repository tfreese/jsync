// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter.impl;

import java.nio.ByteBuffer;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public class ByteBufferAdapter implements DataAdapter<ByteBuffer>
{
    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readByte(java.lang.Object)
     */
    @Override
    public byte readByte(final ByteBuffer source)
    {
        return source.get();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readBytes(java.lang.Object, int)
     */
    @Override
    public byte[] readBytes(final ByteBuffer source, final int length)
    {
        byte[] bytes = new byte[length];
        source.get(bytes);

        return bytes;
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readDouble(java.lang.Object)
     */
    @Override
    public double readDouble(final ByteBuffer source)
    {
        return source.getDouble();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readFloat(java.lang.Object)
     */
    @Override
    public float readFloat(final ByteBuffer source)
    {
        return source.getFloat();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readInteger(java.lang.Object)
     */
    @Override
    public int readInteger(final ByteBuffer source)
    {
        return source.getInt();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readLong(java.lang.Object)
     */
    @Override
    public long readLong(final ByteBuffer source)
    {
        return source.getLong();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeByte(java.lang.Object, byte)
     */
    @Override
    public void writeByte(final ByteBuffer sink, final byte value)
    {
        sink.put(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeBytes(java.lang.Object, byte[])
     */
    @Override
    public void writeBytes(final ByteBuffer sink, final byte[] bytes)
    {
        sink.put(bytes);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeDouble(java.lang.Object, double)
     */
    @Override
    public void writeDouble(final ByteBuffer sink, final double value)
    {
        sink.putDouble(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeFloat(java.lang.Object, float)
     */
    @Override
    public void writeFloat(final ByteBuffer sink, final float value)
    {
        sink.putFloat(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeInteger(java.lang.Object, int)
     */
    @Override
    public void writeInteger(final ByteBuffer sink, final int value)
    {
        sink.putInt(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeLong(java.lang.Object, long)
     */
    @Override
    public void writeLong(final ByteBuffer sink, final long value)
    {
        sink.putLong(value);
    }
}
