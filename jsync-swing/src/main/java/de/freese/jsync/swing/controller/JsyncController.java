// Created: 12.07.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     *
     */
    private Client client;

    /**
     *
     */
    private SyncView syncView;

    /**
     * Erstellt ein neues {@link JsyncController} Object.
     */
    public JsyncController()
    {
        super();
    }

    /**
     * @param syncView {@link SyncView}
     */
    public void init(final SyncView syncView)
    {
        this.syncView = syncView;

        syncView.doOnCompare(button -> button.addActionListener(event -> compare()));
        syncView.doOnSyncronize(button -> button.addActionListener(event -> synchronize()));
    }

    /**
     *
     */
    public void shutdown()
    {
        getLogger().info("shutdown");

        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
        }
    }

    /**
     *
     */
    private void compare()
    {
        Options options = getSyncView().getOptions();
        URI senderUri = getSyncView().getUri(EFileSystem.SENDER);
        URI receiverUri = getSyncView().getUri(EFileSystem.RECEIVER);

        getSyncView().doOnCompare(button -> button.setEnabled(false));
        getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        createNewClient(options, senderUri, receiverUri);

        getSyncView().clearTable();

        // final boolean parallelism = !(senderUri.getScheme().equals("file") && receiverUri.getScheme().equals("file"));
        final boolean parallelism = options.isParallelism();

        ExecutorService executorService = JSyncSwingApplication.getInstance().getExecutorService();

        getSyncView().addProgressBarText(EFileSystem.SENDER, "");
        getSyncView().setProgressBarIndeterminate(EFileSystem.SENDER, true);
        getSyncView().addProgressBarText(EFileSystem.RECEIVER, "");
        getSyncView().setProgressBarIndeterminate(EFileSystem.RECEIVER, true);
        getSyncView().setProgressBarFiles(0);

        SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
        {
            /**
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception
            {
                // Dateien laden
                RunnableFuture<List<SyncItem>> futureSenderItem = createLoadSyncItemsFuture(EFileSystem.SENDER);
                RunnableFuture<List<SyncItem>> futureReceiverItem = createLoadSyncItemsFuture(EFileSystem.RECEIVER);

                if (parallelism)
                {
                    executorService.execute(futureSenderItem);
                }
                else
                {
                    futureSenderItem.run();
                }

                futureReceiverItem.run();

                List<SyncItem> syncItemsSender = futureSenderItem.get();
                List<SyncItem> syncItemsReceiver = futureReceiverItem.get();

                // Merge
                List<SyncPair> syncList = getClient().mergeSyncItems(syncItemsSender, syncItemsReceiver);
                getSyncView().setProgressBarFiles(syncList.size());

                if (!options.isChecksum())
                {
                    syncList.forEach(syncPair -> {
                        syncPair.validateStatus();
                        getSyncView().addSyncPair(syncPair);
                    });

                    return null;
                }

                // Checksum
                for (SyncPair syncPair : syncList)
                {
                    getSyncView().addSyncPair(syncPair);

                    RunnableFuture<?> futureSenderChecksum = createChecksumFuture(EFileSystem.SENDER, syncPair.getSenderItem());
                    RunnableFuture<?> futureReceiverChecksum = createChecksumFuture(EFileSystem.RECEIVER, syncPair.getReceiverItem());

                    if (futureSenderChecksum != null)
                    {
                        if (parallelism)
                        {
                            executorService.execute(futureSenderChecksum);
                        }
                        else
                        {
                            futureSenderChecksum.run();
                        }
                    }

                    if (futureReceiverChecksum != null)
                    {
                        futureReceiverChecksum.run();
                    }

                    if (futureSenderChecksum != null)
                    {
                        futureSenderChecksum.get();
                    }

                    syncPair.validateStatus();
                }

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
            }
        };

        swingWorker.execute();
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<?> createChecksumFuture(final EFileSystem fileSystem, final SyncItem syncItem)
    {
        if ((syncItem == null) || !syncItem.isFile())
        {
            return null;
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
     *
     */
    private void synchronize()
    {
        // JOptionPane.showMessageDialog(JSyncSwingApplication.getInstance().getMainFrame(), "Not implemented !", "Error", JOptionPane.ERROR_MESSAGE);
        SynchronizeWorker worker = new SynchronizeWorker(this);

        worker.execute();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     */
    void createNewClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
        }

        this.client = new DefaultClient(options, senderUri, receiverUri);
        this.client.connectFileSystems();
    }

    /**
     * @return {@link Client}
     */
    Client getClient()
    {
        return this.client;
    }

    /**
     * @param key String
     * @return String
     */
    String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     * @return {@link SyncView}
     */
    SyncView getSyncView()
    {
        return this.syncView;
    }

    /**
     * @param runnable {@link Runnable}
     */
    void runInEdt(final Runnable runnable)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
