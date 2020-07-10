// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.net.URI;
import de.freese.jsync.Options;

/**
 * Factory für den {@link Receiver}.<br>
 * Liefert die konkrete Implementierung gemäß {@link URI}.
 *
 * @author Thomas Freese
 */
public final class ReceiverFactory
{
    /**
     * Liefert die konkrete Implementierung gemäß {@link URI}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     * @return {@link Receiver}
     */
    public static Receiver createFromURI(final Options options, final URI baseUri)
    {
        Receiver receiver = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            receiver = new LocalhostReceiver(options, baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            receiver = new RemoteReceiver(options, baseUri);
        }

        if (receiver == null)
        {
            throw new IllegalStateException("no receiver for URI: " + baseUri);
        }

        return receiver;
    }

    /**
     * Erstellt ein neues {@link ReceiverFactory} Object.
     */
    private ReceiverFactory()
    {
        super();
    }
}
