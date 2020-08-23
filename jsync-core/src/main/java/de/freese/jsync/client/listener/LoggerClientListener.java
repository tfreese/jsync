// Created: 23.11.2018
package de.freese.jsync.client.listener;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Logger}-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
public class LoggerClientListener extends AbstractClientListener
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger("Client");

    /**
     * Erstellt ein neues {@link LoggerClientListener} Object.
     */
    public LoggerClientListener()
    {
        super();
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#copyProgress(de.freese.jsync.Options, de.freese.jsync.model.SyncItem, long)
     */
    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        String message = copyProgressMessage(options, syncItem, bytesTransferred);

        if (message == null)
        {
            return;
        }

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#delete(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void delete(final Options options, final SyncItem syncItem)
    {
        String message = deleteMessage(options, syncItem);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error(final String message, final Throwable th)
    {
        getLogger().error(message, th);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#update(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final Options options, final SyncItem syncItem)
    {
        String message = updateMessage(options, syncItem);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validate(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validate(final Options options, final SyncItem syncItem)
    {
        String message = validateMessage(options, syncItem);

        getLogger().info(message);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }
}
