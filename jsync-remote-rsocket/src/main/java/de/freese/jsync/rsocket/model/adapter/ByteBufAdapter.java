// Created: 04.10.2020
package de.freese.jsync.rsocket.model.adapter;

import de.freese.jsync.model.serializer.adapter.DataAdapter;
import io.netty.buffer.ByteBuf;

/**
 * @author Thomas Freese
 */
public class ByteBufAdapter implements DataAdapter<ByteBuf>
{
    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readByte(java.lang.Object)
     */
    @Override
    public byte readByte(final ByteBuf source)
    {
        return source.readByte();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readBytes(java.lang.Object, int)
     */
    @Override
    public byte[] readBytes(final ByteBuf source, final int length)
    {
        byte[] bytes = new byte[length];
        source.readBytes(bytes);

        return bytes;
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readDouble(java.lang.Object)
     */
    @Override
    public double readDouble(final ByteBuf source)
    {
        return source.readDouble();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readFloat(java.lang.Object)
     */
    @Override
    public float readFloat(final ByteBuf source)
    {
        return source.readFloat();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readInt(java.lang.Object)
     */
    @Override
    public int readInt(final ByteBuf source)
    {
        return source.readInt();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readLong(java.lang.Object)
     */
    @Override
    public long readLong(final ByteBuf source)
    {
        return source.readLong();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeByte(java.lang.Object, byte)
     */
    @Override
    public void writeByte(final ByteBuf sink, final byte value)
    {
        sink.writeByte(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeBytes(java.lang.Object, byte[])
     */
    @Override
    public void writeBytes(final ByteBuf sink, final byte[] bytes)
    {
        sink.writeBytes(bytes);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeDouble(java.lang.Object, double)
     */
    @Override
    public void writeDouble(final ByteBuf sink, final double value)
    {
        sink.writeDouble(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeFloat(java.lang.Object, float)
     */
    @Override
    public void writeFloat(final ByteBuf sink, final float value)
    {
        sink.writeFloat(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeInt(java.lang.Object, int)
     */
    @Override
    public void writeInt(final ByteBuf sink, final int value)
    {
        sink.writeInt(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeLong(java.lang.Object, long)
     */
    @Override
    public void writeLong(final ByteBuf sink, final long value)
    {
        sink.writeLong(value);
    }
}
