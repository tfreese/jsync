// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.function.LongConsumer;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public interface FileSystem
{
    /**
     * Stellt die Verbindung zum Dateisystem her.
     */
    void connect(final URI uri);

    /**
     * Trennt die Verbindung zum Dateisystem.
     */
    void disconnect();

    /**
     * Liefert die Prüfsumme einer Datei.<br>
     */
    String generateChecksum(String baseDir, String relativeFile, LongConsumer consumerChecksumBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses<br>
     */
    Flux<SyncItem> generateSyncItems(String baseDir, boolean followSymLinks, PathFilter pathFilter);
}
