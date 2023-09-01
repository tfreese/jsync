// Created: 23.11.2018
package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class LoggerGeneratorListener extends AbstractGeneratorListener {
    private final Logger logger = LoggerFactory.getLogger("Generator");

    private final String prefix;

    public LoggerGeneratorListener(final String prefix) {
        super();

        this.prefix = Objects.requireNonNull(prefix, "prefix required");
    }

    @Override
    public void checksum(final long bytesRead) {
        // Empty
    }

    @Override
    public void currentItem(final SyncItem syncItem) {
        String message = currentItemMessage(syncItem, this.prefix);

        getLogger().debug(message);
    }

    @Override
    public void itemCount(final Path path, final int itemCount) {
        String message = itemCountMessage(path, itemCount, this.prefix);

        getLogger().info(message);
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
