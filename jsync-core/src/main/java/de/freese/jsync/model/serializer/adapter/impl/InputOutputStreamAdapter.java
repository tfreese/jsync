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
}
