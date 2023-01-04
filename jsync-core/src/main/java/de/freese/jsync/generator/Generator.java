// Created: 22.10.2016
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * The Generator collects all relevant Information of the FileSystem for the chosen {@link Path}.
 *
 * @author Thomas Freese
 */
public interface Generator
{
    /**
     * Erzeugt die Prüfsumme einer Datei.<br>
     *
     * @param consumerChecksumBytesRead {@link LongConsumer}; optional
     */
    String generateChecksum(String baseDir, String relativeFile, LongConsumer consumerChecksumBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     */
    Flux<SyncItem> generateItems(String baseDir, boolean followSymLinks, PathFilter pathFilter);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     */
    default void generateItems(String baseDir, boolean followSymLinks, PathFilter pathFilter, Consumer<SyncItem> consumer)
    {
        generateItems(baseDir, followSymLinks, pathFilter).subscribe(consumer);
    }
}
