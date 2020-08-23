// Created: 22.11.2018
package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.EventListener;

import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;

/**
 * Listener für den {@link Generator}.
 *
 * @author Thomas Freese
 */
public interface GeneratorListener extends EventListener
{
    /**
     * Progress der Prüfsummenbildung.
     *
     * @param bytesRead long
     */
    public void checksum(long bytesRead);

    /**
     * Verzeichniss / Datei, was aktuell bearbeitet wird.
     *
     * @param syncItem {@link SyncItem}
     */
    public void currentItem(SyncItem syncItem);

    /**
     * Anzahl zu verarbeitender Path-Objekte (Verzeichnisse, Dateien).
     *
     * @param path      {@link Path}
     * @param itemCount int
     */
    public void itemCount(Path path, int itemCount);
}
