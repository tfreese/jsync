// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Verarbeitet den Request und Response.
 *
 * @author Thomas Freese
 * @param <T> <T> Type
 */
public interface IoHandler<T>
{
    /**
     *
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * @return {@link Charset}
     */
    public default Charset getCharset()
    {
        return DEFAULT_CHARSET;
    }

    /**
     * Verarbeitet den Request.
     *
     * @param input Object
     */
    public void read(final T input);

    /**
     * Verarbeitet den Response.
     *
     * @param outpuT Object
     */
    public void write(final T outpuT);
}
