// Created: 23.11.2018
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
     * @param syncItem {@link SyncItem}
     * @param prefix String
     *
     * @return String
     */
    protected String currentItemMessage(final SyncItem syncItem, final String prefix)
    {
        return String.format("%s current SyncItem: %s", prefix, syncItem.getRelativePath());
    }

    /**
     * @param path {@link Path}
     * @param itemCount int
     * @param prefix String
     *
     * @return String
     */
    protected String itemCountMessage(final Path path, final int itemCount, final String prefix)
    {
        return String.format("%s size of SyncItems in %s: %d", prefix, path.toString(), itemCount);
    }
}
