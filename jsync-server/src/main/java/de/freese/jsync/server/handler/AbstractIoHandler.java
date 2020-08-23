// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * Verarbeitet den Request und Response.<br>
 * Basis-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see JSyncIoHandler
 */
public abstract class AbstractIoHandler implements IoHandler
{
    /**
     *
     */
    private final ThreadLocal<CharsetDecoder> CHARSET_DECODER = ThreadLocal.withInitial(() ->
    {
        CharsetDecoder decoder = getCharset().newDecoder();

        return decoder;
    });

    /**
     *
     */
    private final ThreadLocal<CharsetEncoder> CHARSET_ENCODER = ThreadLocal.withInitial(() ->
    {
        CharsetEncoder encoder = getCharset().newEncoder();

        return encoder;
    });

    /**
     * Erstellt ein neues {@link AbstractIoHandler} Object.
     */
    public AbstractIoHandler()
    {
        super();
    }

    /**
     * @return {@link CharsetEncoder}
     */
    protected CharsetDecoder getCharsetDecoder()
    {
        return this.CHARSET_DECODER.get().reset();
    }

    /**
     * @return {@link CharsetEncoder}
     */
    protected CharsetEncoder getCharsetEncoder()
    {
        return this.CHARSET_ENCODER.get().reset();
    }
}
