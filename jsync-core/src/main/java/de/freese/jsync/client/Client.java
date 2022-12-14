// Created: 05.04.2018
package de.freese.jsync.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import reactor.core.publisher.Flux;

/**
 * Koordiniert den Abgleich zwischen {@link Sender} und {@link Receiver}.
 *
 * @author Thomas Freese
 */
public interface Client
{
    /**
     * Stellt die Verbindung zu den Dateisystemen her.
     */
    void connectFileSystems();

    /**
     * Trennt die Verbindung zu den Dateisystemen.
     */
    void disconnectFileSystems();

    /**
     * Erzeugt die Prüfsumme einer Datei.<br>
     */
    String generateChecksum(EFileSystem fileSystem, SyncItem syncItem, final LongConsumer consumerChecksumBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     */
    Flux<SyncItem> generateSyncItems(EFileSystem fileSystem, PathFilter pathFilter);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     */
    default void generateSyncItems(final EFileSystem fileSystem, final PathFilter pathFilter, final Consumer<SyncItem> consumer)
    {
        generateSyncItems(fileSystem, pathFilter).subscribe(consumer);
    }

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item nicht im Receiver enthalten, muss es dorthin kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss es dort gelöscht werden.<br>
     */
    List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver);

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     */
    void syncReceiver(List<SyncPair> syncPairs, ClientListener clientListener);
}
