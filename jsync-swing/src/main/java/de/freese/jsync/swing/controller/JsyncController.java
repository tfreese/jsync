/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.controller;

import java.awt.Rectangle;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.LongConsumer;
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
import de.freese.jsync.swing.components.ScheduledAccumulativeRunnable;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     * @author Thomas Freese
     */
    private class SwingWorkerLoadSyncItems extends SwingWorker<List<SyncItem>, Integer>
    {
        /**
         *
         */
        private final EFileSystem fileSystem;

        /**
         *
         */
        private final JProgressBar progressBar;

        /**
         * Erstellt ein neues {@link SwingWorkerLoadSyncItems} Object.
         *
         * @param fileSystem {@link EFileSystem}
         */
        public SwingWorkerLoadSyncItems(final EFileSystem fileSystem)
        {
            super();

            this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem required");
            this.progressBar = EFileSystem.SENDER.equals(fileSystem) ? getSyncView().getSenderProgressBar() : getSyncView().getReceiverProgressBar();
        }

        /**
         * @see javax.swing.SwingWorker#doInBackground()
         */
        @Override
        protected List<SyncItem> doInBackground() throws Exception
        {
            List<SyncItem> syncItems = new ArrayList<>();

            getClient().generateSyncItems(this.fileSystem, syncItem -> {
                syncItems.add(syncItem);

                publish(syncItems.size());
            });

            return syncItems;
        }

        /**
         * @see javax.swing.SwingWorker#done()
         */
        @Override
        protected void done()
        {
            this.progressBar.setIndeterminate(false);
        }

        /**
         * @see javax.swing.SwingWorker#process(java.util.List)
         */
        @Override
        protected void process(final List<Integer> chunks)
        {
            this.progressBar.setString(getMessage("jsync.verarbeite.dateien") + ": " + chunks.get(chunks.size() - 1));
        }
    }

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

        SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
        {
            /**
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception
            {
                CountDownLatch latch = new CountDownLatch(2);

                SwingWorkerLoadSyncItems loadSyncItemsSender = new SwingWorkerLoadSyncItems(EFileSystem.SENDER);
                SwingWorkerLoadSyncItems loadSyncItemsReceiver = new SwingWorkerLoadSyncItems(EFileSystem.RECEIVER);

                // Nacheinander starten.
                loadSyncItemsSender.addPropertyChangeListener(event -> {
                    if (event.getPropertyName().equals("state") && StateValue.DONE.equals(event.getNewValue()))
                    {
                        latch.countDown();
                        loadSyncItemsReceiver.execute();
                    }
                });
                loadSyncItemsReceiver.addPropertyChangeListener(event -> {
                    if (event.getPropertyName().equals("state") && StateValue.DONE.equals(event.getNewValue()))
                    {
                        latch.countDown();
                    }
                });

                loadSyncItemsSender.execute();

                latch.await();

                List<SyncItem> syncItemsSender = loadSyncItemsSender.get();
                List<SyncItem> syncItemsReceiver = loadSyncItemsReceiver.get();

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
                ScheduledExecutorService scheduledExecutorService = JSyncSwingApplication.getInstance().getScheduledExecutorService();
                JProgressBar progressBarSender = getSyncView().getSenderProgressBar();
                JProgressBar progressBarReceiver = getSyncView().getReceiverProgressBar();

                ScheduledAccumulativeRunnable<Integer> sarTableScroll = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarTableScroll.doOnSubmit(chunks -> {
                    int row = chunks.get(chunks.size() - 1);
                    Rectangle rectangle = getSyncView().getSenderTable().getCellRect(row, 0, false);
                    getSyncView().getSenderTable().scrollRectToVisible(rectangle);
                });

                ScheduledAccumulativeRunnable<SyncItem> sarSenderMessage = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarSenderMessage.doOnSubmit(chunks -> {
                    SyncItem syncItem = chunks.get(chunks.size() - 1);
                    progressBarSender.setMinimum(0);
                    progressBarSender.setMaximum((int) syncItem.getSize());
                    progressBarSender.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItem.getRelativePath());
                });
                ScheduledAccumulativeRunnable<SyncItem> sarReceiverMessage = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarReceiverMessage.doOnSubmit(chunks -> {
                    SyncItem syncItem = chunks.get(chunks.size() - 1);
                    progressBarReceiver.setMinimum(0);
                    progressBarReceiver.setMaximum((int) syncItem.getSize());
                    progressBarReceiver.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItem.getRelativePath());
                });

                ScheduledAccumulativeRunnable<Long> sarSenderProgress = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarSenderProgress.doOnSubmit(chunks -> progressBarSender.setValue(chunks.get(chunks.size() - 1).intValue()));
                ScheduledAccumulativeRunnable<Long> sarReceiverProgress = new ScheduledAccumulativeRunnable<>(scheduledExecutorService);
                sarReceiverProgress.doOnSubmit(chunks -> progressBarReceiver.setValue(chunks.get(chunks.size() - 1).intValue()));

                LongConsumer consumerChecksumSender = sarSenderProgress::add;
                LongConsumer consumerChecksumReceiver = sarReceiverProgress::add;

                for (int i = 0; i < syncList.size(); i++)
                {
                    sarTableScroll.add(i);

                    SyncPair syncPair = syncList.get(i);
                    SyncItem syncItemSender = syncPair.getSenderItem();
                    SyncItem syncItemReceiver = syncPair.getReceiverItem();

                    if ((syncItemSender != null) && syncItemSender.isFile())
                    {
                        sarSenderMessage.add(syncItemSender);
                        getClient().generateChecksum(EFileSystem.SENDER, syncItemSender, consumerChecksumSender);
                    }

                    if ((syncItemReceiver != null) && syncItemReceiver.isFile())
                    {
                        sarReceiverMessage.add(syncItemReceiver);
                        getClient().generateChecksum(EFileSystem.RECEIVER, syncItemReceiver, consumerChecksumReceiver);
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
                // getSyncView().getSenderProgressBar().setString(getMessage("jsync.vergleichen.beendet"));
                // getSyncView().getReceiverProgressBar().setString(getMessage("jsync.vergleichen.beendet"));
            }
        };

        swingWorker.execute();
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

        // ActionListener actionListenerEnableCompareButton = event -> {
        // getLogger().info("actionListenerEnableCompareButton");
        // String pathSender = this.syncView.getSenderView().getTextFieldPathPath().getText();
        // String pathReceiver = this.syncView.getReceiverView().getTextFieldPathPath().getText();
        //
        // if ((pathSender != null) && !pathSender.isBlank() && (pathReceiver != null) && !pathReceiver.isBlank())
        // {
        // this.syncView.getButtonCompare().setEnabled(true);
        // }
        // else
        // {
        // this.syncView.getButtonCompare().setEnabled(false);
        // }
        // };
        //
        // this.syncView.getSenderView().getButtonPath().addActionListener(actionListenerEnableCompareButton);
        // this.syncView.getReceiverView().getButtonPath().addActionListener(actionListenerEnableCompareButton);
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
        // TODO
    }
}
