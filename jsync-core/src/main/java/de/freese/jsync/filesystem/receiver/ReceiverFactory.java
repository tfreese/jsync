// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.lang.reflect.Constructor;
import java.net.URI;
import org.slf4j.LoggerFactory;

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
     * @param baseUri {@link URI}
     * @return {@link Receiver}
     */
    public static Receiver createFromURI(final URI baseUri)
    {
        Receiver receiver = null;

        if (baseUri.getScheme().startsWith("file"))
        {
            receiver = new LocalhostReceiver(baseUri);
        }
        else if (baseUri.getScheme().startsWith("jsync"))
        {
            try
            {
                Class<?> clazz = Class.forName("de.freese.jsync.filesystem.receiver.RemoteReceiver");
                Constructor<?> constructor = clazz.getConstructor(URI.class);

                receiver = (Receiver) constructor.newInstance(baseUri);
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
