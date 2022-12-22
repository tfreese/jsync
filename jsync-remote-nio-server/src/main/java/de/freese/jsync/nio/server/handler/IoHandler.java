// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The {@link IoHandler} handles the Request and Response in a separate Thread.<br>
 *
 * @author Thomas Freese
 */
public interface IoHandler<T>
{
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    default Charset getCharset()
    {
        return DEFAULT_CHARSET;
    }

    void read(final T input);

    void write(final T output);
}
