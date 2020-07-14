/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.model.SyncItem;

/**
 * {@link Logger}-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
public class LoggerGeneratorListener extends AbstractGeneratorListener
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger("Generator");

    /**
     *
     */
    private final String prefix;

    /**
     * Erstellt ein neues {@link LoggerGeneratorListener} Object.
     *
     * @param prefix String
     */
    public LoggerGeneratorListener(final String prefix)
    {
        super();

        this.prefix = Objects.requireNonNull(prefix, "prefix required");
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
     */
    @Override
    public void checksum(final long size, final long bytesRead)
    {
        // Empty
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
     */
    @Override
    public void pathCount(final Path path, final int pathCount)
    {
        String message = pathCountMessage(path, pathCount, this.prefix);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#syncItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void syncItem(final SyncItem syncItem)
    {
        String message = processingSyncItemMessage(syncItem, this.prefix);

        getLogger().debug(message);
    }
}
