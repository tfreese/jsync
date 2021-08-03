// Created: 03.08.2021
package de.freese.jsync.swing.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * @author Thomas Freese
 */
public class CompareWorkerList extends AbstractCompareWorker
{
    /**
     * Erstellt ein neues {@link CompareWorkerList} Object.
     *
     * @param controller {@link JsyncController}
     */
    CompareWorkerList(final JsyncController controller)
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
        if ((syncItem == null) || !syncItem.isFile())
        {
            return new FutureTask<>(() -> null);
        }

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
            List<SyncItem> syncItems = new ArrayList<>();

            getClient().generateSyncItems(fileSystem, syncItem -> {
                syncItems.add(syncItem);
                getSyncView().addProgressBarText(fileSystem, getMessage("jsync.files.load") + ": " + syncItems.size());
            });

            getSyncView().setProgressBarIndeterminate(fileSystem, false);

            return syncItems;
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
        RunnableFuture<List<SyncItem>> futureSenderItem = createLoadSyncItemsFuture(EFileSystem.SENDER);
        RunnableFuture<List<SyncItem>> futureReceiverItem = createLoadSyncItemsFuture(EFileSystem.RECEIVER);

        if (isParallel())
        {
            getExecutorService().execute(futureSenderItem);
        }
        else
        {
            futureSenderItem.run();
        }

        futureReceiverItem.run();

        List<SyncItem> syncItemsSender = futureSenderItem.get();
        List<SyncItem> syncItemsReceiver = futureReceiverItem.get();

        // Merge
        List<SyncPair> syncPairs = getClient().mergeSyncItems(syncItemsSender, syncItemsReceiver);

        getSyncView().setProgressBarFilesMax(syncPairs.size());

        if (!getOptions().isChecksum())
        {
            syncPairs.forEach(syncPair -> {
                syncPair.validateStatus();
                getSyncView().addSyncPair(syncPair);
            });

            return null;
        }

        // Checksum
        for (SyncPair syncPair : syncPairs)
        {
            RunnableFuture<Void> futureSenderChecksum = createChecksumFuture(EFileSystem.SENDER, syncPair.getSenderItem());
            RunnableFuture<Void> futureReceiverChecksum = createChecksumFuture(EFileSystem.RECEIVER, syncPair.getReceiverItem());

            if (isParallel())
            {
                getExecutorService().execute(futureSenderChecksum);
            }
            else
            {
                futureSenderChecksum.run();
            }

            futureReceiverChecksum.run();
            futureReceiverChecksum.get();
            futureSenderChecksum.get();

            syncPair.validateStatus();
            getSyncView().addSyncPair(syncPair);
        }

        getSyncView().updateLastEntry();

        return null;
    }
}
