// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

import java.nio.charset.Charset;

/**
 * @author Thomas Freese
 */
public interface DataAdapter
{
    /**
     * @return int
     */
    public int readInt();

    /**
     * @param charset {@link Charset}
     * @return String
     */
    public String readString(Charset charset);

    /**
     * @param value int
     */
    public void writeInt(int value);

    /**
     * @param value {@link CharSequence}
     * @param charset {@link Charset}
     */
    public void writeString(CharSequence value, Charset charset);
}
