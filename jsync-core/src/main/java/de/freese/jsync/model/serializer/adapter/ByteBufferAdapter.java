// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

import java.nio.ByteBuffer;

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
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readInt(java.lang.Object)
     */
    @Override
    public int readInt(final ByteBuffer source)
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
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeInt(java.lang.Object, int)
     */
    @Override
    public void writeInt(final ByteBuffer sink, final int value)
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
