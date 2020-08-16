/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
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
    public void connectFileSystems();

    /**
     * Trennt die Verbindung zu den Dateisystemen.
     */
    public void disconnectFileSystems();

    /**
     * Erzeugt die Prüfsumme einer Datei.<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     * @param consumerBytesRead {@link LongConsumer}; optional
     */
    public void generateChecksum(EFileSystem fileSystem, SyncItem syncItem, final LongConsumer consumerBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param fileSystem {@link EFileSystem}
     * @param consumerSyncItem {@link Consumer}
     */
    public void generateSyncItems(EFileSystem fileSystem, Consumer<SyncItem> consumerSyncItem);

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param syncItemsSender {@link List}
     * @param syncItemsReceiver {@link List}
     * @return {@link List}
     */
    public List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver);

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    public void syncReceiver(List<SyncPair> syncList, ClientListener clientListener);
}
