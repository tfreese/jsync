// Created: 22.10.2016
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Mono;

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
     * @param baseDir String
     * @param relativeFile String
     * @param checksumBytesReadConsumer {@link LongConsumer}; optional
     *
     * @return {@link Mono}
     */
    String generateChecksum(final String baseDir, String relativeFile, final LongConsumer checksumBytesReadConsumer);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumerSyncItem {@link Consumer}
     */
    void generateItems(final String baseDir, boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem);
}
