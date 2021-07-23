package de.freese.jsync.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

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
     * Erzeugt die Prüfsumme einer Datei und speichert diese im {@link SyncItem}.<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     * @param checksumBytesReadConsumer {@link LongConsumer}
     */
    void generateChecksum(EFileSystem fileSystem, SyncItem syncItem, final LongConsumer checksumBytesReadConsumer);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param consumerSyncItem {@link Consumer}
     */
    void generateSyncItems(EFileSystem fileSystem, final Consumer<SyncItem> consumerSyncItem);

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param syncItemsSender {@link List}
     * @param syncItemsReceiver {@link List}
     *
     * @return {@link List}
     */
    List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver);

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     *
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    void syncReceiver(List<SyncPair> syncPairs, ClientListener clientListener);
}
