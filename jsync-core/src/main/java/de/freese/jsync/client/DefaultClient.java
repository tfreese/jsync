// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.client.listener.EmptyClientListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncPairComparator;
import de.freese.jsync.model.SyncStatus;
import reactor.core.publisher.Flux;

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
     */
    public DefaultClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        super(options, senderUri, receiverUri);
    }

    /**
     * @see de.freese.jsync.client.Client#mergeSyncItems(reactor.core.publisher.Flux, reactor.core.publisher.Flux)
     */
    @Override
    public Flux<SyncPair> mergeSyncItems(final Flux<SyncItem> syncItemsSender, final Flux<SyncItem> syncItemsReceiver)
    {
        return Flux.<SyncPair> create(sink -> {
            mergeSyncItems(syncItemsSender.collectList().block(), syncItemsReceiver.collectList().block(), sink::next);
            sink.complete();
        }).sort(new SyncPairComparator());
    }

    /**
     * @see de.freese.jsync.client.Client#mergeSyncItems(java.util.List, java.util.List)
     */
    @Override
    public List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver)
    {
        List<SyncPair> syncPairs = new ArrayList<>();

        mergeSyncItems(syncItemsSender, syncItemsReceiver, syncPairs::add);

        syncPairs.sort(new SyncPairComparator());

        return syncPairs;
    }

    /**
     * @param syncItemsSender {@link List}
     * @param syncItemsReceiver {@link List}
     * @param consumer {@link Consumer}
     */
    private void mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver, final Consumer<SyncPair> consumer)
    {
        // Map der ReceiverItems bauen.
        Map<String, SyncItem> mapReceiver = syncItemsReceiver.stream().collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity()));

        // @formatter:off
        syncItemsSender.stream()
                .map(senderItem -> new SyncPair(senderItem, mapReceiver.remove(senderItem.getRelativePath())))
                .forEach(consumer)
                ;
        // @formatter:on

        // Was jetzt noch in der Receiver-Map drin ist, muss gelöscht werden (source = null).
        mapReceiver.forEach((key, value) -> consumer.accept(new SyncPair(null, value)));
    }

    /**
     * @see de.freese.jsync.client.Client#syncReceiver(java.util.List, de.freese.jsync.client.listener.ClientListener)
     */
    @Override
    public void syncReceiver(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        ClientListener cl = clientListener != null ? clientListener : new EmptyClientListener();

        // Alles rausfiltern was bereits synchronized ist.
        Predicate<SyncPair> isSynchronised = p -> SyncStatus.SYNCHRONIZED.equals(p.getStatus());
        List<SyncPair> sync = syncPairs.stream().filter(isSynchronised.negate()).toList();

        // Löschen
        if (getOptions().isDelete())
        {
            deleteFiles(sync, cl);
            deleteDirectories(sync, cl);
        }

        // Neue oder geänderte Dateien kopieren.
        copyFiles(sync, cl);

        // Aktualisieren von Datei-Attributen.
        updateFiles(sync, cl);

        // Neue leere Verzeichnisse.
        createDirectories(sync, clientListener);

        // Aktualisieren von Verzeichniss-Attributen.
        updateDirectories(sync, cl);
    }
}
