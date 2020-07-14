/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.swing.components.SyncItemTableModel;
import de.freese.jsync.swing.view.SyncView;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     * @author Thomas Freese
     */
    private static class SwingGeneratorListener implements GeneratorListener
    {
        /**
        *
        */
        private final JProgressBar progressBarChecksum;

        /**
         *
         */
        private final JProgressBar progressBarFiles;

        /**
         *
         */
        private final JTable table;

        /**
        *
        */
        private final SyncItemTableModel tableModel;

        /**
         * Erstellt ein neues {@link SwingGeneratorListener} Object.
         *
         * @param progressBarFiles {@link JProgressBar}
         * @param progressBarChecksum {@link JProgressBar}
         * @param table {@link JTable}
         */
        private SwingGeneratorListener(final JProgressBar progressBarFiles, final JProgressBar progressBarChecksum, final JTable table)
        {
            super();

            this.progressBarFiles = progressBarFiles;
            this.progressBarChecksum = progressBarChecksum;
            this.table = table;
            this.tableModel = (SyncItemTableModel) this.table.getModel();
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
         */
        @Override
        public void checksum(final long size, final long bytesRead)
        {
            SwingUtilities.invokeLater(() -> {
                if (bytesRead == 0)
                {
                    this.progressBarChecksum.setMinimum(0);
                    this.progressBarChecksum.setMaximum(100);
                    this.progressBarChecksum.setValue(0);
                    this.progressBarChecksum.setString("Building Checksum...");
                }

                this.progressBarChecksum.setValue((int) JSyncUtils.getPercent(bytesRead, size));

                if (bytesRead == size)
                {
                    this.progressBarChecksum.setString("Building Checksum...finished");
                }
            });
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
         */
        @Override
        public void pathCount(final Path path, final int pathCount)
        {
            SwingUtilities.invokeLater(() -> {
                this.progressBarFiles.setMinimum(0);
                this.progressBarFiles.setMaximum(pathCount);
                this.progressBarFiles.setValue(0);
                this.progressBarFiles.setString("Processing Files...");
            });
        }

        /**
         * @see de.freese.jsync.generator.listener.GeneratorListener#syncItem(de.freese.jsync.model.SyncItem)
         */
        @Override
        public void syncItem(final SyncItem syncItem)
        {
            SwingUtilities.invokeLater(() -> {
                this.tableModel.add(syncItem);

                // Rectangle rectangle = this.table.getCellRect(this.tableModel.getRowCount(), 0, false);
                // this.table.scrollRectToVisible(rectangle);

                this.progressBarFiles.setValue(this.progressBarFiles.getValue() + 1);

                this.progressBarFiles.setString("Processing " + syncItem.getRelativePath());

                if (this.progressBarFiles.getValue() == this.progressBarFiles.getMaximum())
                {
                    this.progressBarFiles.setString("Processing Files...finished");
                }
            });
        }
    }

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
        String pathSender = this.syncView.getSenderView().getTextFieldPathPath().getText();
        String pathReceiver = this.syncView.getReceiverView().getTextFieldPathPath().getText();

        getLogger().info(pathSender);
        getLogger().info(pathReceiver);

        // @formatter:off
        Options options = new Builder()
                .checksum(this.syncView.getCheckBoxChecksum().isSelected())
                .build()
                ;
        // @formatter:on

        JsyncController.this.syncView.getSenderView().getProgressBarChecksum().setVisible(options.isChecksum());
        JsyncController.this.syncView.getReceiverView().getProgressBarChecksum().setVisible(options.isChecksum());

        if ((pathSender != null) && !pathSender.isBlank())
        {
            SyncItemTableModel tableModel = (SyncItemTableModel) this.tableSender.getModel();
            tableModel.clear();

            SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
            {
                /**
                 * @see javax.swing.SwingWorker#doInBackground()
                 */
                @Override
                protected Void doInBackground() throws Exception
                {
                    GeneratorListener listener = new SwingGeneratorListener(JsyncController.this.syncView.getSenderView().getProgressBarFiles(),
                            JsyncController.this.syncView.getSenderView().getProgressBarChecksum(), JsyncController.this.tableSender);

                    Generator generator = new DefaultGenerator();
                    generator.createSyncItems(options, Paths.get(pathSender), listener);

                    return null;
                }

                // /**
                // * @see javax.swing.SwingWorker#process(java.util.List)
                // */
                // @Override
                // protected void process(final List<SyncItem> chunks)
                // {
                // for (SyncItem syncItem : chunks)
                // {
                // JsyncController.this.tableModelSender.add(syncItem);
                // }
                // }
            };

            swingWorker.execute();
        }

        if ((pathReceiver != null) && !pathReceiver.isBlank())
        {
            SyncItemTableModel tableModel = (SyncItemTableModel) this.tableReceiver.getModel();
            tableModel.clear();

            SwingWorker<Void, Void> swingWorker = new SwingWorker<>()
            {
                /**
                 * @see javax.swing.SwingWorker#doInBackground()
                 */
                @Override
                protected Void doInBackground() throws Exception
                {
                    GeneratorListener listener = new SwingGeneratorListener(JsyncController.this.syncView.getReceiverView().getProgressBarFiles(),
                            JsyncController.this.syncView.getReceiverView().getProgressBarChecksum(), JsyncController.this.tableReceiver);

                    Generator generator = new DefaultGenerator();
                    generator.createSyncItems(options, Paths.get(pathSender), listener);

                    return null;
                }
            };

            swingWorker.execute();
        }
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
