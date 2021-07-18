// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;

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
     * @param consumerSyncItem {@link Consumer}
     * @param consumerBytesRead {@link LongConsumer}
     */
    void generateSyncItems(String baseDir, boolean followSymLinks, boolean withChecksum, final Consumer<SyncItem> consumerSyncItem,
                           LongConsumer consumerBytesRead);
}
