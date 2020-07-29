/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.controller;

import java.awt.Rectangle;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.components.AccumulativeRunnable;
import de.freese.jsync.swing.components.ScheduledAccumulativeRunnable;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     *
     */
    private Client client = null;

    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     *
     */
    private SyncView syncView = null;

    /**
     * Erstellt ein neues {@link JsyncController} Object.
     */
    public JsyncController()
    {
        super();
    }

    /**
     *
     */
    private void compare()
    {
        getSyncView().getButtonCompare().setEnabled(false);
        getSyncView().getButtonSyncronize().setEnabled(false);

        // @formatter:off
        Options options = new Builder()
                .checksum(getSyncView().getCheckBoxChecksum().isSelected())
                .build()
                ;
        // @formatter:on

        String pathSender = getSyncView().getSenderTextFieldPath().getText();
        String pathReceiver = getSyncView().getReceiverTextFieldPath().getText();

        // URI senderUri = URI.create(pathSender); // Erzeugt Fehler bei Leerzeichen
        // URI receiverUri = URI.create(pathReceiver);
        URI senderUri = Paths.get(pathSender).toUri();
        URI receiverUri = Paths.get(pathReceiver).toUri();

        createNewClient(options, senderUri, receiverUri);

        getSyncView().getSenderTableModel().clear();
        getSyncView().getReceiverTableModel().clear();

        boolean parallelism = !(senderUri.getScheme().equals("file") && receiverUri.getScheme().equals("file"));

        ExecutorService executorService = JSyncSwingApplication.getInstance().getExecutorService();
        ScheduledExecutorService scheduledExecutorService = JSyncSwingApplication.getInstance().getScheduledExecutorService();
        JProgressBar progressBarSender = getSyncView().getSenderProgressBar();
        JProgressBar progressBarReceiver = getSyncView().getReceiverProgressBar();

        progressBarSender.setString(null);
        progressBarSender.setIndeterminate(true);
        progressBarReceiver.setString(null);
        progressBarReceiver.setIndeterminate(true);

        SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
        {
            /**
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception
            {
                // Dateien laden
                RunnableFuture<List<SyncItem>> futureSenderItem = createLoadSyncItemsFuture(EFileSystem.SENDER, progressBarSender);
                RunnableFuture<List<SyncItem>> futureReceiverItem = createLoadSyncItemsFuture(EFileSystem.RECEIVER, progressBarReceiver);

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

                runInEdt(() -> {
                    getSyncView().getSenderTableModel().addAll(syncList);
                    getSyncView().getReceiverTableModel().addAll(syncList);
                });

                if (!options.isChecksum())
                {
                    getClient().checkSyncStatus(syncList);

                    return null;
                }

                // Checksum
                ScheduledAccumulativeRunnable<Integer> sarTableScroll = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarTableScroll.doOnSubmit(chunks -> {
                    int row = chunks.get(chunks.size() - 1);
                    Rectangle rectangle = getSyncView().getSenderTable().getCellRect(row, 0, false);
                    getSyncView().getSenderTable().scrollRectToVisible(rectangle);
                });

                ScheduledAccumulativeRunnable<SyncItem> sarSenderItem = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarSenderItem.doOnSubmit(chunks -> {
                    SyncItem syncItem = chunks.get(chunks.size() - 1);
                    progressBarSender.setMinimum(0);
                    progressBarSender.setMaximum((int) syncItem.getSize());
                    progressBarSender.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItem.getRelativePath());
                });
                ScheduledAccumulativeRunnable<SyncItem> sarReceiverItem = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarReceiverItem.doOnSubmit(chunks -> {
                    SyncItem syncItem = chunks.get(chunks.size() - 1);
                    progressBarReceiver.setMinimum(0);
                    progressBarReceiver.setMaximum((int) syncItem.getSize());
                    progressBarReceiver.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItem.getRelativePath());
                });

                ScheduledAccumulativeRunnable<Long> sarSenderChecksum = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarSenderChecksum.doOnSubmit(chunks -> progressBarSender.setValue(chunks.get(chunks.size() - 1).intValue()));
                ScheduledAccumulativeRunnable<Long> sarReceiverChecksum = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarReceiverChecksum.doOnSubmit(chunks -> progressBarReceiver.setValue(chunks.get(chunks.size() - 1).intValue()));

                for (int i = 0; i < syncList.size(); i++)
                {
                    sarTableScroll.add(i);

                    SyncPair syncPair = syncList.get(i);

                    RunnableFuture<?> futureSenderChecksum =
                            createChecksumFuture(EFileSystem.SENDER, syncPair.getSenderItem(), sarSenderItem, sarSenderChecksum);
                    RunnableFuture<?> futureReceiverChecksum =
                            createChecksumFuture(EFileSystem.RECEIVER, syncPair.getReceiverItem(), sarReceiverItem, sarReceiverChecksum);

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

                    // System.out.println("JsyncController.compare().new SwingWorker() {...}.doInBackground(): " + i);
                }

                getClient().checkSyncStatus(syncList);

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

                // getSyncView().getSenderProgressBar().setString(getMessage("jsync.vergleichen.beendet"));
                // getSyncView().getReceiverProgressBar().setString(getMessage("jsync.vergleichen.beendet"));
                getSyncView().getButtonCompare().setEnabled(true);
                getSyncView().getButtonSyncronize().setEnabled(true);
            }
        };

        swingWorker.execute();
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @param syncItem {@link SyncItem}
     * @param itemRunnable {@link AccumulativeRunnable}
     * @param checksumRunnable {@link AccumulativeRunnable}
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<?> createChecksumFuture(final EFileSystem fileSystem, final SyncItem syncItem, final AccumulativeRunnable<SyncItem> itemRunnable,
                                                   final AccumulativeRunnable<Long> checksumRunnable)
    {
        if ((syncItem == null) || !syncItem.isFile())
        {
            return null;
        }

        Runnable runnable = () -> {
            itemRunnable.add(syncItem);
            getClient().generateChecksum(fileSystem, syncItem, checksumRunnable::add);
        };

        RunnableFuture<?> future = new FutureTask<>(runnable, null);

        return future;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @param progressBar {@link JProgressBar}
     * @return {@link RunnableFuture}
     */
    private RunnableFuture<List<SyncItem>> createLoadSyncItemsFuture(final EFileSystem fileSystem, final JProgressBar progressBar)
    {
        ScheduledExecutorService scheduledExecutorService = JSyncSwingApplication.getInstance().getScheduledExecutorService();

        ScheduledAccumulativeRunnable<Integer> sarLoadItems = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
        sarLoadItems.doOnSubmit(chunks -> {
            progressBar.setString(getMessage("jsync.verarbeite.dateien") + ": " + chunks.get(chunks.size() - 1));
        });

        Callable<List<SyncItem>> callable = () -> {
            List<SyncItem> syncItems = new ArrayList<>();

            getClient().generateSyncItems(fileSystem, syncItem -> {
                syncItems.add(syncItem);
                sarLoadItems.add((syncItems.size()));
            });

            runInEdt(() -> progressBar.setIndeterminate(false));

            return syncItems;
        };

        RunnableFuture<List<SyncItem>> future = new FutureTask<>(callable);

        return future;
    }

    /**
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     */
    private void createNewClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
        }

        this.client = new DefaultClient(options, senderUri, receiverUri, null);
        this.client.connectFileSystems();
    }

    /**
     * @return {@link Client}
     */
    private Client getClient()
    {
        return this.client;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param key String
     * @return String
     */
    private String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     * @return {@link SyncView}
     */
    private SyncView getSyncView()
    {
        return this.syncView;
    }

    /**
     * @param syncView {@link SyncView}
     */
    public void init(final SyncView syncView)
    {
        this.syncView = syncView;

        syncView.getButtonCompare().addActionListener(event -> compare());
        syncView.getButtonSyncronize().addActionListener(event -> synchronize());

        // syncView.getSenderTable().setAutoscrolls(true);
        // syncView.getSenderTable().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        syncView.getSenderTable().getColumnModel().getColumn(0).setPreferredWidth(1000);
        syncView.getSenderTable().getColumnModel().getColumn(1).setMinWidth(100);

        syncView.getReceiverTable().getColumnModel().getColumn(0).setPreferredWidth(1000);
        syncView.getReceiverTable().getColumnModel().getColumn(1).setMinWidth(100);
    }

    /**
     * @param runnable {@link Runnable}
     */
    private void runInEdt(final Runnable runnable)
    {
        SwingUtilities.invokeLater(runnable);
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
    private void synchronize()
    {
        JOptionPane.showMessageDialog(JSyncSwingApplication.getInstance().getMainFrame(), "Not implemented !", "Error", JOptionPane.ERROR_MESSAGE);
    }
}
