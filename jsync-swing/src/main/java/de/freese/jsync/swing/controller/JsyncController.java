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

        if ((pathSender != null) && !pathSender.isBlank())
        {
            SyncItemTableModel tableModel = (SyncItemTableModel) this.tableSender.getModel();
            tableModel.clear();

            SwingWorker<Void, SyncItem> swingWorker = new SwingWorker<>()
            {
                /**
                 * @see javax.swing.SwingWorker#doInBackground()
                 */
                @Override
                protected Void doInBackground() throws Exception
                {
                    Generator generatorSender = new DefaultGenerator();
                    generatorSender.createSyncItems(options, Paths.get(pathSender), new GeneratorListener()
                    {
                        /**
                         * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
                         */
                        @Override
                        public void checksum(final long size, final long bytesRead)
                        {
                            SwingUtilities.invokeLater(() -> {
                                JProgressBar progressBar = JsyncController.this.syncView.getSenderView().getProgressBarChecksum();

                                if (bytesRead == 0)
                                {
                                    progressBar.setMinimum(0);
                                    progressBar.setMaximum(100);
                                    progressBar.setValue(0);
                                    progressBar.setString("Building Checksum...");
                                }

                                progressBar.setValue((int) JSyncUtils.getPercent(bytesRead, size));

                                if (bytesRead == size)
                                {
                                    progressBar.setString("Building Checksum...finished");
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
                                JProgressBar progressBar = JsyncController.this.syncView.getSenderView().getProgressBarFiles();
                                progressBar.setMinimum(0);
                                progressBar.setMaximum(pathCount);
                                progressBar.setValue(0);
                                progressBar.setString("Processing Files...");
                            });
                        }

                        /**
                         * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
                         */
                        @Override
                        public void processingSyncItem(final SyncItem syncItem)
                        {
                            SwingUtilities.invokeLater(() -> {
                                tableModel.add(syncItem);

                                // Rectangle rectangle = JsyncController.this.tableSender.getCellRect(tableModel.getRowCount(), 0, false);
                                // JsyncController.this.tableSender.scrollRectToVisible(rectangle);

                                JProgressBar progressBar = JsyncController.this.syncView.getSenderView().getProgressBarFiles();
                                progressBar.setValue(progressBar.getValue() + 1);

                                progressBar.setString("Processing " + syncItem.getRelativePath());

                                if (progressBar.getValue() == progressBar.getMaximum())
                                {
                                    progressBar.setString("Processing Files...finished");
                                }
                            });
                        }
                    });

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

            SwingWorker<Void, SyncItem> swingWorker = new SwingWorker<>()
            {
                /**
                 * @see javax.swing.SwingWorker#doInBackground()
                 */
                @Override
                protected Void doInBackground() throws Exception
                {
                    Generator generatorSender = new DefaultGenerator();
                    generatorSender.createSyncItems(options, Paths.get(pathReceiver), new GeneratorListener()
                    {
                        /**
                         * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
                         */
                        @Override
                        public void checksum(final long size, final long bytesRead)
                        {
                            // TODO Auto-generated method stub
                        }

                        /**
                         * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
                         */
                        @Override
                        public void pathCount(final Path path, final int pathCount)
                        {
                            // TODO Auto-generated method stuby
                        }

                        /**
                         * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
                         */
                        @Override
                        public void processingSyncItem(final SyncItem syncItem)
                        {
                            tableModel.add(syncItem);

                            // Rectangle rectangle = JsyncController.this.tableReceiver.getCellRect(tableModel.getRowCount(), 0, false);
                            // JsyncController.this.tableReceiver.scrollRectToVisible(rectangle);
                        }
                    });

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
