// Created: 23.11.2018
package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.Objects;

import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.model.SyncItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long)
     */
    @Override
    public void checksum(final long bytesRead)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#currentItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void currentItem(final SyncItem syncItem)
    {
        String message = currentItemMessage(syncItem, this.prefix);

        getLogger().debug(message);
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#itemCount(java.nio.file.Path, int)
     */
    @Override
    public void itemCount(final Path path, final int itemCount)
    {
        String message = itemCountMessage(path, itemCount, this.prefix);

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
