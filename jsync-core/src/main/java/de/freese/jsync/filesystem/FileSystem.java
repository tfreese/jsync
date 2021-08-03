// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public interface FileSystem
{
    /**
     * Stellt die Verbindung zum Dateisystem her.
     *
     * @param uri {@link URI}
     */
    void connect(final URI uri);

    /**
     * Trennt die Verbindung zum Dateisystem.
     */
    void disconnect();

    /**
     * Liefert die Prüfsumme einer Datei.<br>
     *
     * @param baseDir String
     * @param relativeFile String
     * @param consumerChecksumBytesRead {@link LongConsumer}
     *
     * @return String
     */
    String generateChecksum(String baseDir, String relativeFile, LongConsumer consumerChecksumBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     *
     * @return {@link Flux}
     */
    Flux<SyncItem> generateSyncItems(String baseDir, boolean followSymLinks);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumer {@link Consumer}
     */
    void generateSyncItems(String baseDir, boolean followSymLinks, final Consumer<SyncItem> consumer);
}
