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
     *
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     * @param consumerChecksumBytesRead {@link LongConsumer}
     *
     * @return String
     */
    String generateChecksum(EFileSystem fileSystem, SyncItem syncItem, final LongConsumer consumerChecksumBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link Flux}
     */
    Flux<SyncItem> generateSyncItems(EFileSystem fileSystem);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param consumer {@link Consumer}
     */
    void generateSyncItems(EFileSystem fileSystem, final Consumer<SyncItem> consumer);

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item nicht im Receiver enthalten, muss es dorthin kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss es dort gelöscht werden.<br>
     *
     * @param syncItemsSender {@link Flux}
     * @param syncItemsReceiver {@link Flux}
     *
     * @return {@link Flux}
     */
    Flux<SyncPair> mergeSyncItems(final Flux<SyncItem> syncItemsSender, final Flux<SyncItem> syncItemsReceiver);

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item nicht im Receiver enthalten, muss es dorthin kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss es dort gelöscht werden.<br>
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
