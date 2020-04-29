// Created: 05.04.2018
package de.freese.jsync.filesystem.source;

import java.net.URI;
import de.freese.jsync.Options;

/**
 * Factory für den {@link Source}.<br>
 * Liefert die konkrete Implementierung gemäß {@link URI}.
 *
 * @author Thomas Freese
 */
public class SourceFactory
{
    /**
     * Liefert die konkrete Implementierung gemäß {@link URI}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     * @return {@link Source}
     */
    public static Source createSourceFromURI(final Options options, final URI baseUri)
    {
        Source source = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            source = new LocalhostSource(options, baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            source = new RemoteSource(options, baseUri);
        }

        if (source == null)
        {
            throw new IllegalStateException("no sender for URI: " + baseUri);
        }

        return source;
    }
}
