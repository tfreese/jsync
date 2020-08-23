// Created: 23.11.2018
package de.freese.jsync.client.listener;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.JSyncUtils;

/**
 * Basis-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractClientListener implements ClientListener
{
    /**
     * Erstellt ein neues {@link AbstractClientListener} Object.
     */
    public AbstractClientListener()
    {
        super();
    }

    /**
     * Hängt bei Bedarf den String " (dry-run)" an die Message.
     *
     * @param options {@link Options}
     * @param message String
     *
     * @return String
     */
    protected String appendDryRun(final Options options, final String message)
    {
        String msg = message;

        if (options.isDryRun())
        {
            msg += " (dry-run)";
        }

        return msg;
    }

    /**
     * @param options  {@link Options}
     * @param syncItem {@link SyncItem}
     *
     * @return String
     */
    protected String copyMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("copy: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options          {@link Options}
     * @param syncItem         {@linkSyncItem}
     * @param bytesTransferred long
     *
     * @return String
     */
    protected String copyProgressMessage(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        float percent = JSyncUtils.getPercent(bytesTransferred, syncItem.getSize());
        String message = null;

        if ((bytesTransferred == 0) || ((percent % 10) == 0))
        {
            message = String.format("copy %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(bytesTransferred), percent);
        }

        return message;
    }

    /**
     * @param options  {@link Options}
     * @param syncItem {@link SyncItem}
     *
     * @return String
     */
    protected String deleteMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("delete: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options  {@link Options}
     * @param syncItem {@link SyncItem}
     *
     * @return String
     */
    protected String updateMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("update attributes: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options  {@link Options}
     * @param syncItem {@link SyncItem}
     *
     * @return String
     */
    protected String validateMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("validate: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }
}
