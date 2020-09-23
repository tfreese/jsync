// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class ByteBufferAdapter implements DataAdapter
{
    /**
     *
     */
    private final ByteBuffer buffer;

    /**
     * Erstellt ein neues {@link ByteBufferAdapter} Object.
     *
     * @param buffer {@link ByteBuffer}
     */
    public ByteBufferAdapter(final ByteBuffer buffer)
    {
        super();

        this.buffer = Objects.requireNonNull(buffer, "buffer required");
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.DataAdapter#readInt()
     */
    @Override
    public int readInt()
    {
        return this.buffer.getInt();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.DataAdapter#readString(java.nio.charset.Charset)
     */
    @Override
    public String readString(final Charset charset)
    {
        int length = this.buffer.getInt();

        if (length == -1)
        {
            return null;
        }
        else if (length == 0)
        {
            return "";
        }

        byte[] bytes = new byte[length];
        this.buffer.get(bytes);
        String text = new String(bytes, charset);

        return text;
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.DataAdapter#writeInt(int)
     */
    @Override
    public void writeInt(final int value)
    {
        this.buffer.putInt(value);
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.DataAdapter#writeString(java.lang.CharSequence, java.nio.charset.Charset)
     */
    @Override
    public void writeString(final CharSequence value, final Charset charset)
    {
        if (value == null)
        {
            this.buffer.putInt(-1);
            return;
        }
        else if (value.length() == 0)
        {
            this.buffer.putInt(0);
            return;
        }

        byte[] bytes = value.toString().getBytes(charset);
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);
    }
}
