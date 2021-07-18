// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.net.URI;
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
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     * @param withChecksum boolean
     * @param consumerBytesRead {@link LongConsumer}
     *
     * @return {@link Flux}
     */
    Flux<SyncItem> generateSyncItems(String baseDir, boolean followSymLinks, boolean withChecksum, LongConsumer consumerBytesRead);
}
