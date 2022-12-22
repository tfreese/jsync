// Created: 03.08.2021
package de.freese.jsync.swing.controller;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import reactor.util.function.Tuple2;

/**
 * @author Thomas Freese
 */
public class CompareWorker extends AbstractWorker<Void, Void>
{
    CompareWorker(final JSyncController controller)
    {
        super(controller);

        getSyncView().doOnCompare(button -> button.setEnabled(false));
        getSyncView().doOnSynchronize(button -> button.setEnabled(false));

        getSyncView().clearTable();

        getSyncView().setProgressBarText(EFileSystem.SENDER, "");
        getSyncView().setProgressBarText(EFileSystem.RECEIVER, "");

        getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, true);
        getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, true);

        getSyncView().setProgressBarFilesMax(0);
    }

    protected RunnableFuture<List<SyncItem>> createFutureSyncItems(final EFileSystem fileSystem, final PathFilter pathFilter)
    {
        // @formatter:off
        Callable<List<SyncItem>> callable = () -> getClient().generateSyncItems(fileSystem, pathFilter)
                .index()
                //.delayElements(Duration.ofMillis(10))
                .doOnNext(tuple -> getSyncView().setProgressBarText(fileSystem, getMessage("jsync.files.load") + ": " + (tuple.getT1() + 1)))
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
        PathFilter pathFilter = getSyncView().getPathFilter();

        RunnableFuture<List<SyncItem>> futureSenderItems = createFutureSyncItems(EFileSystem.SENDER, pathFilter);
        RunnableFuture<List<SyncItem>> futureReceiverItems = createFutureSyncItems(EFileSystem.RECEIVER, pathFilter);

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

        // Fill GUI.
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

        getSyncView().updateLastEntry();

        // Wait until all GUI-Events are processed.
        // TODO BAD SOLUTION !!!
        TimeUnit.MILLISECONDS.sleep(200);

        return null;
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        getLogger().info("{}", ByteBufferPool.DEFAULT);

        try
        {
            get();
        }
        catch (Exception ex)
        {
            getLogger().error(ex.getMessage(), ex);
        }

        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSynchronize(button -> button.setEnabled(true));

        getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, false);
        getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, false);

        getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
    }

    private RunnableFuture<Void> createFutureChecksum(final EFileSystem fileSystem, final SyncItem syncItem)
    {
        if (syncItem == null)
        {
            return new FutureTask<>(() -> null);
        }

        Runnable runnable = () ->
        {
            getSyncView().setProgressBarText(fileSystem, getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());

            String checksum = getClient().generateChecksum(fileSystem, syncItem, bytesRead ->
            {
                if (bytesRead == 0)
                {
                    getSyncView().setProgressBarIndeterminate(fileSystem, false);
                    getSyncView().setProgressBarMinMaxText(fileSystem, 0, (int) syncItem.getSize(),
                            getMessage("jsync.options.checksum") + ": " + syncItem.getRelativePath());
                }

                getSyncView().setProgressBarValue(fileSystem, (int) bytesRead);
            });

            syncItem.setChecksum(checksum);
        };

        return new FutureTask<>(runnable, null);
    }
}
