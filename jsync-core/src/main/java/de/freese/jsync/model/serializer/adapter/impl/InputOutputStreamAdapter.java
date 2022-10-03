// Created: 30.09.22
package de.freese.jsync.model.serializer.adapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public class InputOutputStreamAdapter implements DataAdapter<OutputStream, InputStream>
{
    @Override
    public byte readByte(final InputStream source)
    {
        try
        {
            return (byte) source.read();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public byte[] readBytes(final InputStream source, final int length)
    {
        try
        {
            return source.readNBytes(length);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public double readDouble(final InputStream source)
    {
        long longValue = readLong(source);

        return Double.longBitsToDouble(longValue);
    }

    @Override
    public float readFloat(final InputStream source)
    {
        int intValue = readInteger(source);

        return Float.intBitsToFloat(intValue);
    }

    @Override
    public int readInteger(final InputStream source)
    {
        byte[] bytes = readBytes(source, 4);

        // @formatter:off
        return ((bytes[0] & 0xFF) << 24)
                + ((bytes[1] & 0xFF) << 16)
                + ((bytes[2] & 0xFF) << 8)
                + (bytes[3] & 0xFF)
                ;
        // @formatter:on
    }

    @Override
    public long readLong(final InputStream source)
    {
        byte[] bytes = readBytes(source, 8);

        // @formatter:off
        return ((long) (bytes[0] & 0xFF) << 56)
                + ((long) (bytes[1] & 0xFF) << 48)
                + ((long) (bytes[2] & 0xFF) << 40)
                + ((long) (bytes[3] & 0xFF) << 32)
                + ((long) (bytes[4] & 0xFF) << 24)
                + ((long) (bytes[5] & 0xFF) << 16)
                + ((long) (bytes[6] & 0xFF) << 8)
                + ((long) bytes[7] & 0xFF)
                ;
        // @formatter:on
    }

    @Override
    public void writeByte(final OutputStream sink, final byte value)
    {
        try
        {
            sink.write(value);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeBytes(final OutputStream sink, final byte[] bytes)
    {
        try
        {
            sink.write(bytes);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeDouble(final OutputStream sink, final double value)
    {
        long longValue = Double.doubleToRawLongBits(value);

        writeLong(sink, longValue);
    }

    @Override
    public void writeFloat(final OutputStream sink, final float value)
    {
        int intValue = Float.floatToRawIntBits(value);

        writeInteger(sink, intValue);
    }

    @Override
    public void writeInteger(final OutputStream sink, final int value)
    {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);

        writeBytes(sink, bytes);
    }

    @Override
    public void writeLong(final OutputStream sink, final long value)
    {
        byte[] bytes = new byte[8];

        bytes[0] = (byte) ((value >> 56) & 0xFF);
        bytes[1] = (byte) ((value >> 48) & 0xFF);
        bytes[2] = (byte) ((value >> 40) & 0xFF);
        bytes[3] = (byte) ((value >> 32) & 0xFF);
        bytes[4] = (byte) ((value >> 24) & 0xFF);
        bytes[5] = (byte) ((value >> 16) & 0xFF);
        bytes[6] = (byte) ((value >> 8) & 0xFF);
        bytes[7] = (byte) (value & 0xFF);

        writeBytes(sink, bytes);
    }
}
