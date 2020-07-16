/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.client;

import java.util.List;
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
     * @param sender {@link Sender}
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     * @param withChecksum boolean
     * @throws Exception Falls was schief geht.
     */
    public void syncReceiver(Sender sender, Receiver receiver, List<SyncPair> syncList, final boolean withChecksum) throws Exception;
}
