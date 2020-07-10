/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.client;

import java.util.List;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncPair;

/**
 * Koordiniert den Abgleich zwischen {@link Sender} und {@link Receiver}.
 *
 * @author Thomas Freese
 */
public interface Client
{
    /**
     * Ermittelt die Differenzen von Quelle und Ziel.
     *
     * @param sender {@link Sender}
     * @param senderListener {@link GeneratorListener}; optional.
     * @param receiver {@link Receiver}
     * @param receiverListener {@link GeneratorListener}; optional.
     * @return {@link List}
     * @throws Exception Falls was schief geht.
     */
    public List<SyncPair> createSyncList(Sender sender, GeneratorListener senderListener, Receiver receiver, GeneratorListener receiverListener) throws Exception;

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     *
     * @param sender {@link Sender}
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     * @throws Exception Falls was schief geht.
     */
    public void syncReceiver(Sender sender, Receiver receiver, List<SyncPair> syncList) throws Exception;
}
