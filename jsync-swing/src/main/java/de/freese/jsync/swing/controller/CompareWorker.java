// Created: 03.08.2021
package de.freese.jsync.swing.controller;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.utils.pool.ByteBufferPool;
import reactor.util.function.Tuple2;

/**
 * @author Thomas Freese
 */
public class CompareWorker extends AbstractWorker<Void, Void>
{
    /**
     * Erstellt ein neues {@link CompareWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    CompareWorker(final JsyncController controller)
    {
        super(controller);

        getSyncView().doOnCompare(button -> button.setEnabled(false));
        getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        getSyncView().clearTable();

        getSyncView().addProgressBarText(EFileSystem.SENDER, "");
        getSyncView().addProgressBarText(EFileSystem.RECEIVER, "");

        getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, true);
        getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, true);

        getSyncView().setProgressBarFilesMax(0);
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     *
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<Void> createFutureChecksum(final EFileSystem fileSystem, final SyncItem syncItem)
    {
        if (syncItem == null)
        {
            return new FutureTask<>(() -> null);
        }

        Runnable runnable = () -> {
            getSyncView().addProgressBarText(fileSystem, getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());

            String checksum = getClient().generateChecksum(fileSystem, syncItem, bytesRead -> {
                if (bytesRead == 0)
                {
                    getSyncView().setProgressBarIndeterminate(fileSystem, false);
                    getSyncView().addProgressBarMinMaxText(fileSystem, 0, (int) syncItem.getSize(),
                            getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());
                }

                getSyncView().addProgressBarValue(fileSystem, (int) bytesRead);
            });

            syncItem.setChecksum(checksum);
        };

        return new FutureTask<>(runnable, null);
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link RunnableFuture}
     */
    protected RunnableFuture<List<SyncItem>> createFutureSyncItems(final EFileSystem fileSystem)
    {
        // @formatter:off
        Callable<List<SyncItem>> callable = () -> getClient().generateSyncItems(fileSystem)
                .index()
                //.delayElements(Duration.ofMillis(10))
                .doOnNext(tuple -> getSyncView().addProgressBarText(fileSystem, getMessage("jsync.files.load") + ": " + (tuple.getT1() + 1)))
                .map(Tuple2::getT2)
                .collectList()
                .block();
        // @formatter:on

        return new FutureTask<>(callable);
    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        // Dateien laden
        RunnableFuture<List<SyncItem>> futureSenderItems = createFutureSyncItems(EFileSystem.SENDER);
        RunnableFuture<List<SyncItem>> futureReceiverItems = createFutureSyncItems(EFileSystem.RECEIVER);

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

        // GUI befüllen.
        for (SyncPair syncPair : syncPairs)
        {
            getSyncView().addSyncPair(syncPair);

            // Checksum
            if (getOptions().isChecksum() && syncPair.isFile())
            {
                RunnableFuture<Void> futureSenderChecksum = createFutureChecksum(EFileSystem.SENDER, syncPair.getSenderItem());
                RunnableFuture<Void> futureReceiverChecksum = createFutureChecksum(EFileSystem.RECEIVER, syncPair.getReceiverItem());

                if (isParallel())
                {
                    getExecutorService().execute(futureSenderChecksum);
                    getExecutorService().execute(futureReceiverChecksum);
                }
                else
                {
                    futureSenderChecksum.run();
                    futureReceiverChecksum.run();
                }

                futureSenderChecksum.get();
                futureReceiverChecksum.get();
            }

            syncPair.validateStatus();
        }

        // getSyncView().updateLastEntry();

        return null;
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        ByteBufferPool pool = ByteBufferPool.getInstance();
        getLogger().info("ByteBufferPool: created={}, free={}, peak={}", pool.getCreated(), pool.getFree(), pool.getPeak());

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

        getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, false);
        getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, false);

        getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
    }
}
