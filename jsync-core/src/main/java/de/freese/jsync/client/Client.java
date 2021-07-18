package de.freese.jsync.client;

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
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param consumerBytesRead {@link LongConsumer}
     *
     * @return {@link Flux}
     */
    Flux<SyncItem> generateSyncItems(EFileSystem fileSystem, final LongConsumer consumerBytesRead);

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param syncItemsSender {@link Flux}
     * @param syncItemsReceiver {@link Flux}
     *
     * @return {@link Flux}
     */
    Flux<SyncPair> mergeSyncItems(final Flux<SyncItem> syncItemsSender, final Flux<SyncItem> syncItemsReceiver);

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     *
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    void syncReceiver(Flux<SyncPair> syncFlux, ClientListener clientListener);
}
