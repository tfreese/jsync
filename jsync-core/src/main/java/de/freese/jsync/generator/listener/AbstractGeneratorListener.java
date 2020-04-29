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
     * @param path {@link Path}
     * @param pathCount int
     * @param prefix String
     * @return String
     */
    protected String pathCountMessage(final Path path, final int pathCount, final String prefix)
    {
        String message = String.format("%s size of SyncItems in %s: %d", prefix, path.toString(), pathCount);

        return message;
    }

    /**
     * @param syncItem {@link SyncItem}
     * @param prefix String
     * @return String
     */
    protected String processingSyncItemMessage(final SyncItem syncItem, final String prefix)
    {
        String message = String.format("%s current SyncItem: %s", prefix, syncItem);

        return message;
    }
}
