// Created: 03.08.2021
package de.freese.jsync.swing.controller;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * @author Thomas Freese
 */
public class CompareWorkerReactive extends AbstractCompareWorker
{
    /**
     * Erstellt ein neues {@link CompareWorkerReactive} Object.
     *
     * @param controller {@link JsyncController}
     */
    CompareWorkerReactive(final JsyncController controller)
    {
        super(controller);
    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        // Dateien laden

        Flux<SyncItem> syncItemsSender = getClient().generateSyncItems(EFileSystem.SENDER);

        if (isParallel())
        {
            syncItemsSender = syncItemsSender.publishOn(Schedulers.parallel());
        }

        // @formatter:off
        syncItemsSender = syncItemsSender
            .index()
            .doOnNext(tuple -> getSyncView().addProgressBarText(EFileSystem.SENDER, getMessage("jsync.files.load") + ": " + (tuple.getT1() + 1)))
            .map(Tuple2::getT2)
            .doFinally(signal -> getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, false))
            ;
        // @formatter:on

        // @formatter:off
        Flux<SyncItem> syncItemsReceiver = getClient().generateSyncItems(EFileSystem.RECEIVER)
                .index()
                .doOnNext(tuple -> getSyncView().addProgressBarText(EFileSystem.RECEIVER, getMessage("jsync.files.load") + ": " + (tuple.getT1() + 1)))
                .map(Tuple2::getT2)
                .doFinally(signal -> getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, false))
                ;
        // @formatter:on

        // Merge
        Flux<SyncPair> syncPairs = getClient().mergeSyncItems(syncItemsSender, syncItemsReceiver);

        getSyncView().setProgressBarFilesMax(syncPairs.count().block().intValue());

        if (!getOptions().isChecksum())
        {
            syncPairs.subscribe(syncPair -> {
                syncPair.validateStatus();
                getSyncView().addSyncPair(syncPair);
            });

            getSyncView().updateLastEntry();

            return null;
        }

        // Checksum
        syncPairs.subscribe(syncPair -> {
            Mono<SyncItem> syncItemSender = Mono.justOrEmpty(syncPair.getSenderItem());

            // if (isParallel())
            // {
            // syncItemSender = syncItemSender.publishOn(Schedulers.parallel());
            // }

            // @formatter:off
            syncItemSender = syncItemSender
                    .filter(SyncItem::isFile)
                    .doOnNext(syncItem -> {
                        EFileSystem fileSystem = EFileSystem.SENDER;

                        getSyncView().addProgressBarMinMaxText(fileSystem, 0, (int) syncItem.getSize(), getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());
                        getClient().generateChecksum(fileSystem, syncItem, bytesRead -> getSyncView().addProgressBarValue(fileSystem, (int) bytesRead));
                    });
            // @formatter:on

            // @formatter:off
            Mono<SyncItem> syncItemReceiver = Mono.justOrEmpty(syncPair.getReceiverItem())
                    .filter(SyncItem::isFile)
                    .doOnNext(syncItem -> {
                        EFileSystem fileSystem = EFileSystem.RECEIVER;

                        getSyncView().addProgressBarMinMaxText(fileSystem, 0, (int) syncItem.getSize(), getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());
                        getClient().generateChecksum(fileSystem, syncItem, bytesRead -> getSyncView().addProgressBarValue(fileSystem, (int) bytesRead));
                    });
            // @formatter:on

            syncItemSender.subscribe();
            syncItemReceiver.subscribe();

            syncPair.validateStatus();
            getSyncView().addSyncPair(syncPair);
        });

        getSyncView().updateLastEntry();

        return null;
    }
}
