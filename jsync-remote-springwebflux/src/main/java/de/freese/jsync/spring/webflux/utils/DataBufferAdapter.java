// Created: 07.10.2020
package de.freese.jsync.spring.webflux.utils;

import org.springframework.core.io.buffer.DataBuffer;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public class DataBufferAdapter implements DataAdapter<DataBuffer>
{
    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readByte(java.lang.Object)
     */
    @Override
    public byte readByte(final DataBuffer source)
    {
        return source.read();
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readBytes(java.lang.Object, int)
     */
    @Override
    public byte[] readBytes(final DataBuffer source, final int length)
    {
        byte[] bytes = new byte[length];
        source.read(bytes);

        return bytes;
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readDouble(java.lang.Object)
     */
    @Override
    public double readDouble(final DataBuffer source)
    {
        long longValue = readLong(source);

        return Double.longBitsToDouble(longValue);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterRead#readFloat(java.lang.Object)
     */
    @Override
    public float readFloat(final DataBuffer source)
    {
        int intValue = readInt(source);

        return Float.intBitsToFloat(intValue);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readInt(java.lang.Object)
     */
    @Override
    public int readInt(final DataBuffer source)
    {
        byte[] bytes = new byte[4];
        source.read(bytes);

        // @formatter:off
        int value = ((bytes[0] & 0xFF) << 24)
                    + ((bytes[1] & 0xFF) << 16)
                    + ((bytes[2] & 0xFF) << 8)
                    + (bytes[3] & 0xFF);
        // @formatter:on

        return value;
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#readLong(java.lang.Object)
     */
    @Override
    public long readLong(final DataBuffer source)
    {
        byte[] bytes = new byte[8];
        source.read(bytes);

        // @formatter:off
        long value = ((long) (bytes[0] & 0xFF) << 56)
                    + ((long) (bytes[1] & 0xFF) << 48)
                    + ((long) (bytes[2] & 0xFF) << 40)
                    + ((long) (bytes[3] & 0xFF) << 32)
                    + ((long) (bytes[4] & 0xFF) << 24)
                    + ((bytes[5] & 0xFF) << 16)
                    + ((bytes[6] & 0xFF) << 8)
                    + (bytes[7] & 0xFF);
        // @formatter:off

        return value;
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeByte(java.lang.Object, byte)
     */
    @Override
    public void writeByte(final DataBuffer sink, final byte value)
    {
        sink.write(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeBytes(java.lang.Object, byte[])
     */
    @Override
    public void writeBytes(final DataBuffer sink, final byte[] bytes)
    {
        sink.write(bytes);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeDouble(java.lang.Object, double)
     */
    @Override
    public void writeDouble(final DataBuffer sink, final double value)
    {
        long longValue = Double.doubleToRawLongBits(value);

        writeLong(sink, longValue);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapterWrite#writeFloat(java.lang.Object, float)
     */
    @Override
    public void writeFloat(final DataBuffer sink, final float value)
    {
        int intValue = Float.floatToRawIntBits(value);

        writeInt(sink, intValue);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeInt(java.lang.Object, int)
     */
    @Override
    public void writeInt(final DataBuffer sink, final int value)
    {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) ((value >>> 24) & 0xFF);
        bytes[1] = (byte) ((value >>> 16) & 0xFF);
        bytes[2] = (byte) ((value >>> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);

        sink.write(bytes);
    }

    /**
     * @see de.freese.jsync.model.serializer.adapter.DataAdapter#writeLong(java.lang.Object, long)
     */
    @Override
    public void writeLong(final DataBuffer sink, final long value)
    {
        byte[] bytes = new byte[8];

        bytes[0] = (byte) ((value >>> 56) & 0xFF);
        bytes[1] = (byte) ((value >>> 48) & 0xFF);
        bytes[2] = (byte) ((value >>> 40) & 0xFF);
        bytes[3] = (byte) ((value >>> 32) & 0xFF);
        bytes[4] = (byte) ((value >>> 24) & 0xFF);
        bytes[5] = (byte) ((value >>> 16) & 0xFF);
        bytes[6] = (byte) ((value >>> 8) & 0xFF);
        bytes[7] = (byte) (value & 0xFF);

        sink.write(bytes);
    }
}
