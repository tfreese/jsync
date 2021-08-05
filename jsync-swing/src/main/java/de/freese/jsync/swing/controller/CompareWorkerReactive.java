// Created: 03.08.2021
package de.freese.jsync.swing.controller;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     *
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<Void> createChecksumFuture(final EFileSystem fileSystem, final SyncItem syncItem)
    {
        Runnable runnable = () -> {
            getSyncView().addProgressBarMinMaxText(fileSystem, 0, (int) syncItem.getSize(),
                    getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());

            getClient().generateChecksum(fileSystem, syncItem, bytesRead -> getSyncView().addProgressBarValue(fileSystem, (int) bytesRead));
        };

        return new FutureTask<>(runnable, null);
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<List<SyncItem>> createLoadSyncItemsFuture(final EFileSystem fileSystem)
    {
        Callable<List<SyncItem>> callable = () -> {
            // @formatter:off
            Flux<SyncItem> syncItemsFlux = getClient().generateSyncItems(fileSystem)
                    .index()
                    .delayElements(Duration.ofMillis(100))
                    .doOnNext(tuple -> getSyncView().addProgressBarText(fileSystem, getMessage("jsync.files.load") + ": " + (tuple.getT1() + 1)))
                    .map(Tuple2::getT2)
                    .doFinally(signal -> getSyncView().setProgressBarIndeterminate(fileSystem, false))
                    ;
            // @formatter:on

            return syncItemsFlux.collectList().block();
        };

        return new FutureTask<>(callable);
    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        // Dateien laden
        RunnableFuture<List<SyncItem>> futureSenderItems = createLoadSyncItemsFuture(EFileSystem.SENDER);
        RunnableFuture<List<SyncItem>> futureReceiverItems = createLoadSyncItemsFuture(EFileSystem.RECEIVER);

        if (isParallel())
        {
            getExecutorService().execute(futureSenderItems);
            getExecutorService().execute(futureReceiverItems);
        }
        else
        {
            futureSenderItems.run();
            futureReceiverItems.run();
        }

        List<SyncItem> syncItemsSender = futureSenderItems.get();
        List<SyncItem> syncItemsReceiver = futureReceiverItems.get();

        // Merge
        List<SyncPair> syncPairs = getClient().mergeSyncItems(syncItemsSender, syncItemsReceiver);

        getSyncView().setProgressBarFilesMax(syncPairs.size());

        if (!getOptions().isChecksum())
        {
            syncPairs.forEach(syncPair -> {
                syncPair.validateStatus();
                getSyncView().addSyncPair(syncPair);
            });

            getSyncView().updateLastEntry();

            return null;
        }

        // Checksum
        syncPairs.forEach(syncPair -> {
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
