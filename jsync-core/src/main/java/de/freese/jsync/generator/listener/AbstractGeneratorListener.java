/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;

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
     * @param relativePath String
     * @param prefix String
     * @return String
     */
    protected String currentMetaMessage(final String relativePath, final String prefix)
    {
        String message = String.format("%s current Meta-Info: %s", prefix, relativePath);

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
