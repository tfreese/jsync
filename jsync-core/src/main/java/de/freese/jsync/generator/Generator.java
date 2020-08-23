// Created: 22.10.2016
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;

/**
 * Der Generator sammelt alle relevanten Informationens des Dateisystems für den gewählten {@link Path}.
 *
 * @author Thomas Freese
 */
public interface Generator
{
    /**
     * Erzeugt die Prüfsumme einer Datei.<br>
     *
     * @param baseDir           String
     * @param relativeFile      String
     * @param consumerBytesRead {@link LongConsumer}; optional
     *
     * @return String
     */
    public String generateChecksum(final String baseDir, String relativeFile, final LongConsumer consumerBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param baseDir          String
     * @param followSymLinks   boolean
     * @param consumerSyncItem {@link Consumer}
     */
    public void generateItems(final String baseDir, boolean followSymLinks, Consumer<SyncItem> consumerSyncItem);
}
