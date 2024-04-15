// Created: 05.04.2018
package de.freese.jsync.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * Coordinates {@link Sender} and {@link Receiver}.
 *
 * @author Thomas Freese
 */
public interface Client {
    void connectFileSystems();

    void disconnectFileSystems();

    String generateChecksum(EFileSystem fileSystem, SyncItem syncItem, LongConsumer consumerChecksumBytesRead);

    Flux<SyncItem> generateSyncItems(EFileSystem fileSystem, PathFilter pathFilter);

    default void generateSyncItems(final EFileSystem fileSystem, final PathFilter pathFilter, final Consumer<SyncItem> consumer) {
        generateSyncItems(fileSystem, pathFilter).subscribe(consumer);
    }

    /**
     * Merges the {@link SyncItem} from {@link Sender} and {@link Receiver}.<br>
     * The Entries of the Sender are the Reference.<br>
     * Is an Item not existing in the Receiver, it must be copied.<br>
     * Is an Item only in the Receiver, it must be deleted.<br>
     */
    List<SyncPair> mergeSyncItems(List<SyncItem> syncItemsSender, List<SyncItem> syncItemsReceiver);

    void syncReceiver(List<SyncPair> syncPairs, ClientListener clientListener);
}
