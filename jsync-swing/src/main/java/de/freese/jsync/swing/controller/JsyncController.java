/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.controller;

import java.awt.Rectangle;
import java.util.function.LongConsumer;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.swing.components.SyncItemTableModel;
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
                .checksum(this.syncView.getCheckBoxChecksum().isSelected())
                .build()
                ;
        // @formatter:on

        String pathSender = this.syncView.getSenderView().getTextFieldPathPath().getText();
        String pathReceiver = this.syncView.getReceiverView().getTextFieldPathPath().getText();

        createSyncItems(pathSender, options, this.tableSender, this.syncView.getSenderView().getProgressBarFiles(),
                this.syncView.getSenderView().getProgressBarChecksum());
        createSyncItems(pathReceiver, options, this.tableReceiver, this.syncView.getReceiverView().getProgressBarFiles(),
                this.syncView.getReceiverView().getProgressBarChecksum());
    }

    /**
     * @param basePath String
     * @param options {@link Options}
     * @param table {@link JTable}
     * @param progressBarFiles {@link JProgressBar}
     * @param progressBarChecksum {@link JProgressBar}
     */
    private void createSyncItems(final String basePath, final Options options, final JTable table, final JProgressBar progressBarFiles,
                                 final JProgressBar progressBarChecksum)
    {
        getLogger().info(basePath);

        SyncItemTableModel tableModel = (SyncItemTableModel) table.getModel();
        tableModel.clear();

        progressBarChecksum.setVisible(options.isChecksum());

        if ((basePath == null) || basePath.isBlank())
        {
            return;
        }

        LongConsumer consumerCheckSum = bytesRead -> {
            SwingUtilities.invokeLater(() -> {
                // this.progressBarChecksum.setValue((int) JSyncUtils.getPercent(bytesRead, size));
                progressBarChecksum.setValue((int) bytesRead);

                if (bytesRead == progressBarChecksum.getMaximum())
                {
                    progressBarChecksum.setString("Building Checksum...finished");
                }
            });
        };

        progressBarFiles.setString("Loading Files...");

        SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
        {
            /**
             * @see javax.swing.SwingWorker#doInBackground()
             */
            @Override
            protected Void doInBackground() throws Exception
            {
                // GeneratorListener listener = new SwingGeneratorListener(progressBarFiles, progressBarChecksum, table);
                // generator.generateItems(path, options.isFollowSymLinks(), listener);

                Generator generator = new DefaultGenerator();
                generator.generateItems(basePath, options.isFollowSymLinks(), syncItem -> {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.add(syncItem);
                        Rectangle rectangle = table.getCellRect(tableModel.getRowCount(), 0, false);
                        table.scrollRectToVisible(rectangle);

                        progressBarFiles.setValue(progressBarFiles.getValue() + 1);
                        progressBarFiles.setString("Processing " + syncItem.getRelativePath());

                        if (syncItem.isFile() && options.isChecksum())
                        {
                            progressBarChecksum.setMinimum(0);
                            progressBarChecksum.setMaximum((int) syncItem.getSize());
                            progressBarChecksum.setString("Building Checksum...");
                        }
                    });

                    if (options.isChecksum() && syncItem.isFile())
                    {
                        generator.generateChecksum(basePath, syncItem.getRelativePath(), consumerCheckSum);
                    }
                });

                return null;
            }
        };

        swingWorker.execute();
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param syncView {@link SyncView}
     */
    public void init(final SyncView syncView)
    {
        this.syncView = syncView;

        syncView.getButtonCompare().addActionListener(event -> compare());
        syncView.getButtonSyncronize().addActionListener(event -> synchronize());

        this.tableSender = syncView.getSenderView().getTable();
        this.tableSender.setModel(new SyncItemTableModel());
        // this.tableSender.setAutoscrolls(true);
        // this.tableSender.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.tableSender.getColumnModel().getColumn(0).setPreferredWidth(1000);
        this.tableSender.getColumnModel().getColumn(1).setMinWidth(100);

        this.tableReceiver = syncView.getReceiverView().getTable();
        this.tableReceiver.setModel(new SyncItemTableModel());
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
     *
     */
    public void shutdown()
    {
        // Empty
    }

    /**
    *
    */
    private void synchronize()
    {
        // TODO
    }
}
