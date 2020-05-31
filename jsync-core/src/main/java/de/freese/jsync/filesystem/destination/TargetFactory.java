// Created: 05.04.2018
package de.freese.jsync.filesystem.destination;

import java.net.URI;
import de.freese.jsync.Options;

/**
 * Factory für den {@link Target}.<br>
 * Liefert die konkrete Implementierung gemäß {@link URI}.
 *
 * @author Thomas Freese
 */
public class TargetFactory
{
    /**
     * Liefert die konkrete Implementierung gemäß {@link URI}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     * @return {@link Target}
     */
    public static Target createReceiverFromURI(final Options options, final URI baseUri)
    {
        Target target = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            target = new LocalhostTarget(options, baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            target = new RemoteTarget(options, baseUri);
        }

        if (target == null)
        {
            throw new IllegalStateException("no receiver for URI: " + baseUri);
        }

        return target;
    }
}
