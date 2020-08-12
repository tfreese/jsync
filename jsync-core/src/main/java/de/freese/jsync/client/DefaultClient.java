// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncPairComparator;
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
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param clientListener {@link ClientListener}
     */
    public DefaultClient(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener)
    {
        super(options, senderUri, receiverUri, clientListener);
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

        fileList.sort(new SyncPairComparator());

        return fileList;
    }

    /**
     * @see de.freese.jsync.client.Client#syncReceiver(java.util.List)
     */
    @Override
    public void syncReceiver(final List<SyncPair> syncList)
    {
        getClientListener().syncStartInfo();

        // Alles rausfiltern was bereits synchronized ist.
        Predicate<SyncPair> isSynchronised = p -> SyncStatus.SYNCHRONIZED.equals(p.getStatus());
        List<SyncPair> list = syncList.stream().filter(isSynchronised.negate()).collect(Collectors.toList());

        // Löschen
        if (getOptions().isDelete())
        {
            deleteFiles(list);
            deleteDirectories(list);
        }

        // Neue oder geänderte Dateien kopieren.
        copyFiles(list);

        // Aktualisieren von Datei-Attributen.
        updateFiles(list);

        // Aktualisieren von Verzeichniss-Attributen.
        updateDirectories(list);

        getClientListener().syncFinishedInfo();
    }
}
