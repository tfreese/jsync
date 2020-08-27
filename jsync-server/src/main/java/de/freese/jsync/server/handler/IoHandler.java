// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Verarbeitet den Request und Response.
 *
 * @author Thomas Freese
 */
public interface IoHandler
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
     * @param selectionKey {@link SelectionKey}
     *
     * @throws Exception Falls was schief geht.
     */
    public void read(final SelectionKey selectionKey) throws Exception;

    /**
     * Verarbeitet den Response.
     *
     * @param selectionKey {@link SelectionKey}
     *
     * @throws Exception Falls was schief geht.
     */
    public void write(final SelectionKey selectionKey) throws Exception;
}
