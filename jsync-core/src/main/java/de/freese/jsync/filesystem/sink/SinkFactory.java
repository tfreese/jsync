// Created: 05.04.2018
package de.freese.jsync.filesystem.sink;

import java.net.URI;
import de.freese.jsync.Options;

/**
 * Factory für den {@link Sink}.<br>
 * Liefert die konkrete Implementierung gemäß {@link URI}.
 *
 * @author Thomas Freese
 */
public final class SinkFactory
{
    /**
     * Liefert die konkrete Implementierung gemäß {@link URI}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     * @return {@link Sink}
     */
    public static Sink createSinkFromURI(final Options options, final URI baseUri)
    {
        Sink sink = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            sink = new LocalhostSink(options, baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            sink = new RemoteSink(options, baseUri);
        }

        if (sink == null)
        {
            throw new IllegalStateException("no sink for URI: " + baseUri);
        }

        return sink;
    }

    /**
     * Erstellt ein neues {@link SinkFactory} Object.
     */
    private SinkFactory()
    {
        super();
    }
}
