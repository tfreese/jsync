// Created: 23.11.2018
package de.freese.jsync.client.listener;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractClientListener implements ClientListener
{
    protected String appendDryRun(final Options options, final String message)
    {
        String msg = message;

        if (options.isDryRun())
        {
            msg += " (dry-run)";
        }

        return msg;
    }

    protected String checksumProgressMessage(final Options options, final SyncItem syncItem, final long bytesRead)
    {
        double percent = JSyncUtils.getPercent(bytesRead, syncItem.getSize());
        String message = null;

        if ((bytesRead == 0) || Double.compare(percent % 10, 0D) == 0)
        {
            message = String.format("checksum %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(bytesRead), percent);
        }

        return message;
    }

    protected String copyMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("copy: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    protected String copyProgressMessage(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        double percent = JSyncUtils.getPercent(bytesTransferred, syncItem.getSize());
        String message = null;

        if ((bytesTransferred == 0) || Double.compare(percent % 10, 0D) == 0)
        {
            message = String.format("copy %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(bytesTransferred), percent);
        }

        return message;
    }

    protected String deleteMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("delete: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    protected String updateMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("update attributes: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    protected String validateMessage(final Options options, final SyncItem syncItem)
    {
        String message = String.format("validate: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }
}
