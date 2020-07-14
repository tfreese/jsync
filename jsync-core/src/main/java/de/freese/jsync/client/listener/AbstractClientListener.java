/**
 * Created: 23.11.2018
 */

package de.freese.jsync.client.listener;

import java.util.ArrayList;
import java.util.List;
import de.freese.jsync.Options;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
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
     * @param options {@link Options}
     * @param syncItem {@link FileSyncItem}
     * @return String
     */
    protected String copyFileMessage(final Options options, final FileSyncItem syncItem)
    {
        String message = String.format("copy: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param syncItem {@link FileSyncItem}
     * @param size long
     * @param bytesTransferred long
     * @return String
     */
    protected String copyFileProgressMessage(final FileSyncItem syncItem, final long size, final long bytesTransferred)
    {
        String message = String.format("copy %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(bytesTransferred),
                JSyncUtils.getPercent(bytesTransferred, size));

        return message;
    }

    /**
     * @param options {@link Options}
     * @param directory String
     * @return String
     */
    protected String createDirectoryMessage(final Options options, final String directory)
    {
        String message = String.format("create: %s", directory);

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options {@link Options}
     * @param directory String
     * @return String
     */
    protected String deleteDirectoryMessage(final Options options, final String directory)
    {
        String message = String.format("delete: %s", directory);

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options {@link Options}
     * @param file String
     * @return String
     */
    protected String deleteFileMessage(final Options options, final String file)
    {
        String message = String.format("delete: %s", file);

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options {@link Options}
     * @return String
     */
    protected List<String> dryRunInfoMessage(final Options options)
    {
        List<String> messagesList = new ArrayList<>();

        if (options.isDryRun())
        {
            messagesList.add("**********************************************");
            messagesList.add("Dry-Run: No file operations will be executed !");
            messagesList.add("**********************************************");
        }

        return messagesList;
    }

    /**
     * @return String
     */
    protected String generatingFileListInfoMessage()
    {
        String message = "Generating FileList...";

        return message;
    }

    /**
     * @return String
     */
    protected String syncFinishedInfoMessage()
    {
        String message = "syncing process finished";

        return message;
    }

    /**
     * @return String
     */
    protected String syncStartInfoMessage()
    {
        String message = "start syncing process";

        return message;
    }

    /**
     * @param options {@link Options}
     * @param syncItem {@link DirectorySyncItem}
     * @return String
     */
    protected String updateDirectoryMessage(final Options options, final DirectorySyncItem syncItem)
    {
        String message = String.format("update attributes: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options {@link Options}
     * @param syncItem {@link FileSyncItem}
     * @return String
     */
    protected String updateFileMessage(final Options options, final FileSyncItem syncItem)
    {
        String message = String.format("update attributes: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }

    /**
     * @param options {@link Options}
     * @param syncItem {@link FileSyncItem}
     * @return String
     */
    protected String validateFileMessage(final Options options, final FileSyncItem syncItem)
    {
        String message = String.format("validate: %s", syncItem.getRelativePath());

        message = appendDryRun(options, message);

        return message;
    }
}
