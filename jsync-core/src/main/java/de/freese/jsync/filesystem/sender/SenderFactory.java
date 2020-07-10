// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.net.URI;
import de.freese.jsync.Options;

/**
 * Factory für den {@link Sender}.<br>
 * Liefert die konkrete Implementierung gemäß {@link URI}.
 *
 * @author Thomas Freese
 */
public final class SenderFactory
{
    /**
     * Liefert die konkrete Implementierung gemäß {@link URI}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     * @return {@link Sender}
     */
    public static Sender createFromURI(final Options options, final URI baseUri)
    {
        Sender sender = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            sender = new LocalhostSender(options, baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            sender = new RemoteSender(options, baseUri);
        }

        if (sender == null)
        {
            throw new IllegalStateException("no sender for URI: " + baseUri);
        }

        return sender;
    }

    /**
     * Erstellt ein neues {@link SenderFactory} Object.
     */
    private SenderFactory()
    {
        super();
    }
}
