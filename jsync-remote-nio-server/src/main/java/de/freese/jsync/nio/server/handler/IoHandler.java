// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Verarbeitet den Request und Response.
 *
 * @param <T> <T> Type
 *
 * @author Thomas Freese
 */
public interface IoHandler<T>
{
    /**
     *
     */
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * @return {@link Charset}
     */
    default Charset getCharset()
    {
        return DEFAULT_CHARSET;
    }

    /**
     * Verarbeitet den Request.
     *
     * @param input Object
     */
    void read(final T input);

    /**
     * Verarbeitet den Response.
     *
     * @param output Object
     */
    void write(final T output);
}
