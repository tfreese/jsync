/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import de.freese.jsync.model.SyncItem;

/**
 * Basis-Implementierung des {@link GeneratorListener}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractGeneratorListener implements GeneratorListener
{
    /**
     * Erstellt ein neues {@link AbstractGeneratorListener} Object.
     */
    public AbstractGeneratorListener()
    {
        super();
    }

    /**
     * @param syncItem {@link SyncItem}
     * @param prefix String
     * @return String
     */
    protected String currentItemMessage(final SyncItem syncItem, final String prefix)
    {
        String message = String.format("%s current SyncItem: %s", prefix, syncItem.getRelativePath());

        return message;
    }

    /**
     * @param path {@link Path}
     * @param itemCount int
     * @param prefix String
     * @return String
     */
    protected String itemCountMessage(final Path path, final int itemCount, final String prefix)
    {
        String message = String.format("%s size of SyncItems in %s: %d", prefix, path.toString(), itemCount);

        return message;
    }
}
