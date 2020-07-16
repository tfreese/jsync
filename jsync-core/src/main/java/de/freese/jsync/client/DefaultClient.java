// Created: 05.04.2018
package de.freese.jsync.client;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;

/**
 * Default-Implementierung des {@link Client}.
 *
 * @author Thomas Freese
 */
public class DefaultClient extends AbstractClient
{
    /**
     * Erzeugt eine neue Instanz von {@link DefaultClient}.
     *
     * @param options {@link Options}
     * @param clientListener {@link ClientListener}
     */
    public DefaultClient(final Options options, final ClientListener clientListener)
    {
        super(options, clientListener);
    }

    /**
     * @see de.freese.jsync.client.Client#mergeSyncItems(java.util.List, java.util.List)
     */
    @Override
    public List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver)
    {
        // Map der ReceiverItems bauen.
        Map<String, SyncItem> mapReceiver = syncItemsReceiver.stream().collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity()));

        // @formatter:off
        List<SyncPair> fileList = syncItemsSender.stream()
                .map(senderItem -> new SyncPair(senderItem, mapReceiver.remove(senderItem.getRelativePath())))
                .collect(Collectors.toList());
        // @formatter:on

        // Was jetzt noch in der Receiver-Map drin ist, muss gelöscht werden (source = null).
        mapReceiver.forEach((key, value) -> fileList.add(new SyncPair(null, value)));

        // SyncStatus ermitteln.
        // @formatter:off
        fileList.stream()
                .peek(SyncPair::validateStatus)
                .forEach(getClientListener()::debugSyncPair);
        // @formatter:on

        return fileList;
    }

    /**
     * @see de.freese.jsync.client.Client#syncReceiver(de.freese.jsync.filesystem.sender.Sender, de.freese.jsync.filesystem.receiver.Receiver, java.util.List,
     *      boolean)
     */
    @Override
    public void syncReceiver(final Sender sender, final Receiver receiver, final List<SyncPair> syncList, final boolean withChecksum) throws Exception
    {
        getClientListener().syncStartInfo();

        // Alles rausfiltern was bereits synchronized ist.
        Predicate<SyncPair> isSynchronised = p -> SyncStatus.SYNCHRONIZED.equals(p.getStatus());
        List<SyncPair> list = syncList.stream().filter(isSynchronised.negate()).collect(Collectors.toList());

        // Löschen
        if (getOptions().isDelete())
        {
            deleteFiles(receiver, list);
            deleteDirectories(receiver, list);
        }

        // Neue Verzeichnisse erstellen.
        createDirectories(receiver, list);

        // Neue oder geänderte Dateien kopieren.
        copyFiles(sender, receiver, list, withChecksum);

        // Aktualisieren von Datei-Attributen.
        updateFiles(receiver, list);

        // Aktualisieren von Verzeichniss-Attributen.
        updateDirectories(receiver, list);

        getClientListener().syncFinishedInfo();
    }
}
