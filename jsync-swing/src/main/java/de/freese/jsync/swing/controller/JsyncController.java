/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.controller;

import java.awt.Rectangle;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongConsumer;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
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
    *
    */
    private JTable tableReceiver = null;

    /**
     *
     */
    private JTable tableSender = null;

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
        getClient().connectFileSystems();

        JProgressBar progressBarSender = getSyncView().getSenderProgressBar();
        JProgressBar progressBarReceiver = getSyncView().getReceiverProgressBar();

        SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
        {
            /**
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception
            {
                runInEdt(() -> {
                    getSyncView().getSenderTableModel().clear();
                    getSyncView().getReceiverTableModel().clear();
                });

                // Sender
                runInEdt(() -> {
                    progressBarSender.setString(null);
                    progressBarSender.setIndeterminate(true);
                });

                List<SyncItem> syncItemsSender = new ArrayList<>();
                getClient().generateSyncItemsSender(syncItem -> {
                    syncItemsSender.add(syncItem);

                    if ((syncItemsSender.size() % 10) == 0)
                    {
                        runInEdt(() -> progressBarSender.setString(getMessage("jsync.verarbeite.dateien") + ": " + syncItemsSender.size()));
                    }
                });

                // Receiver
                runInEdt(() -> {
                    progressBarSender.setIndeterminate(false);
                    progressBarReceiver.setString(null);
                    progressBarReceiver.setIndeterminate(true);
                });

                List<SyncItem> syncItemsReceiver = new ArrayList<>();
                getClient().generateSyncItemsReceiver(syncItem -> {
                    syncItemsReceiver.add(syncItem);

                    if ((syncItemsReceiver.size() % 10) == 0)
                    {
                        runInEdt(() -> progressBarReceiver.setString(getMessage("jsync.verarbeite.dateien") + ": " + syncItemsReceiver.size()));
                    }
                });

                // Merge
                List<SyncPair> syncList = getClient().mergeSyncItems(syncItemsSender, syncItemsReceiver);

                runInEdt(() -> {
                    progressBarReceiver.setIndeterminate(false);
                    getSyncView().getSenderTableModel().addAll(syncList);
                    getSyncView().getReceiverTableModel().addAll(syncList);
                });

                if (!options.isChecksum())
                {
                    return null;
                }

                LongConsumer consumerChecksumSender = bytesRead -> {
                    SwingUtilities.invokeLater(() -> progressBarSender.setValue((int) bytesRead));
                };

                LongConsumer consumerChecksumReceiver = bytesRead -> {
                    SwingUtilities.invokeLater(() -> progressBarReceiver.setValue((int) bytesRead));
                };

                AtomicInteger atomicInteger = new AtomicInteger(0);

                for (SyncPair syncPair : syncList)
                {
                    runInEdt(() -> {
                        Rectangle rectangle = getSyncView().getSenderTable().getCellRect(atomicInteger.get(), 0, false);
                        getSyncView().getSenderTable().scrollRectToVisible(rectangle);
                    });

                    atomicInteger.incrementAndGet();
                    SyncItem syncItemSender = syncPair.getSenderItem();
                    SyncItem syncItemReceiver = syncPair.getReceiverItem();

                    if ((syncItemSender != null) && syncItemSender.isFile())
                    {
                        runInEdt(() -> {
                            progressBarSender.setMinimum(0);
                            progressBarSender.setMaximum((int) syncItemSender.getSize());
                            progressBarSender.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItemSender.getRelativePath());
                        });

                        getClient().generateChecksumSender(syncItemSender, consumerChecksumSender);
                    }

                    if ((syncItemReceiver != null) && syncItemReceiver.isFile())
                    {
                        runInEdt(() -> {
                            progressBarReceiver.setMinimum(0);
                            progressBarReceiver.setMaximum((int) syncItemReceiver.getSize());
                            progressBarReceiver.setString(getMessage("jsync.pruefsumme.erstelle") + ": " + syncItemReceiver.getRelativePath());
                        });

                        getClient().generateChecksumReceiver(syncItemReceiver, consumerChecksumReceiver);
                    }
                }

                return null;
            }

            /**
             * @see javax.swing.SwingWorker#done()
             */
            @Override
            protected void done()
            {
                progressBarSender.setString(getMessage("jsync.vergleichen.beendet"));
                progressBarReceiver.setString(getMessage("jsync.vergleichen.beendet"));
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

        this.tableSender = syncView.getSenderTable();
        // this.tableSender.setAutoscrolls(true);
        // this.tableSender.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.tableSender.getColumnModel().getColumn(0).setPreferredWidth(1000);
        this.tableSender.getColumnModel().getColumn(1).setMinWidth(100);

        this.tableReceiver = syncView.getReceiverTable();
        this.tableReceiver.getColumnModel().getColumn(0).setPreferredWidth(1000);
        this.tableReceiver.getColumnModel().getColumn(1).setMinWidth(100);

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
