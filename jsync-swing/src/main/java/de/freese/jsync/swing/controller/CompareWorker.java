// Created: 16.08.2020
package de.freese.jsync.swing.controller;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author Thomas Freese
 */
class CompareWorker extends AbstractWorker<Void, Void>
{
    /**
     * Erstellt ein neues {@link CompareWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    CompareWorker(final JsyncController controller)
    {
        super(controller);

        controller.getSyncView().doOnCompare(button -> button.setEnabled(false));
        controller.getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        controller.getSyncView().clearTable();

        controller.getSyncView().addProgressBarText(EFileSystem.SENDER, "");
        controller.getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, true);
        controller.getSyncView().addProgressBarText(EFileSystem.RECEIVER, "");
        controller.getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, true);
        controller.getSyncView().setProgressBarFiles(0);
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link RunnableFuture}
     */
    private Flux<SyncItem> createLoadSyncItemsFuture(final EFileSystem fileSystem)
    {
        AtomicInteger counter = new AtomicInteger(0);

        // @formatter:off
        Flux<SyncItem> flux = getClient().generateSyncItems(fileSystem, i -> {})
                .doOnNext(syncItem -> getSyncView().addProgressBarText(fileSystem, getMessage("jsync.files.load") + ": " + counter.addAndGet(1)))
                .doFinally(signal -> getSyncView().setProgressBarIndeterminate(fileSystem, false))
                ;
        // @formatter:on

        return flux;

    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        // Dateien laden
        Flux<SyncItem> senderItems = createLoadSyncItemsFuture(EFileSystem.SENDER);
        Flux<SyncItem> receiverItems = createLoadSyncItemsFuture(EFileSystem.RECEIVER);

        if (isParallel())
        {
            receiverItems = receiverItems.publishOn(Schedulers.boundedElastic());
        }

        // Merge
        Flux<SyncPair> syncList = getClient().mergeSyncItems(senderItems, receiverItems);

        long count = syncList.count().block();
        getSyncView().setProgressBarFiles((int) count);

        syncList.subscribe(syncPair -> {
            syncPair.validateStatus();
            getSyncView().addSyncPair(syncPair);
        });

        getSyncView().updateLastEntry();

        return null;
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        try
        {
            get();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSyncronize(button -> button.setEnabled(true));
        getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
    }
}
