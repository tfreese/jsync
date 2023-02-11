// Created: 23.11.2018
package de.freese.jsync.generator.listener;

import java.nio.file.Path;

import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public abstract class AbstractGeneratorListener implements GeneratorListener {
    protected String currentItemMessage(final SyncItem syncItem, final String prefix) {
        return String.format("%s current SyncItem: %s", prefix, syncItem.getRelativePath());
    }

    protected String itemCountMessage(final Path path, final int itemCount, final String prefix) {
        return String.format("%s size of SyncItems in %s: %d", prefix, path.toString(), itemCount);
    }
}
