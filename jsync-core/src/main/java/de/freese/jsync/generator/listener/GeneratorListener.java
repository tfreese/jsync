// Created: 22.11.2018
package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.EventListener;

import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public interface GeneratorListener extends EventListener {
    /**
     * Progress of the Checksum creation.
     */
    void checksum(long bytesRead);

    /**
     * Current File / Directory.
     */
    void currentItem(SyncItem syncItem);

    /**
     * Number of Path-Objekts (Files / Directories).
     */
    void itemCount(Path path, int itemCount);
}
