// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.lang.reflect.Constructor;
import java.net.URI;
import org.slf4j.LoggerFactory;
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
            try
            {
                Class<?> clazz = Class.forName("de.freese.jsync.filesystem.receiver.RemoteReceiver");
                Constructor<?> constructor = clazz.getConstructor(Options.class, URI.class);

                receiver = (Receiver) constructor.newInstance(options, baseUri);
            }
            catch (Exception ex)
            {
                LoggerFactory.getLogger(ReceiverFactory.class).error("ClassNotFound: {}", ex.getMessage());
            }
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
