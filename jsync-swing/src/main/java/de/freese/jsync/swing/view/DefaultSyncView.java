// Created: 12.08.2020
package de.freese.jsync.swing.view;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.GbcBuilder;
import de.freese.jsync.swing.components.AccumulativeRunnable;
import de.freese.jsync.swing.components.DocumentListenerAdapter;
import de.freese.jsync.swing.components.ScheduledAccumulativeRunnable;
import de.freese.jsync.swing.components.SyncListTableModel;

/**
 * @author Thomas Freese
 */
public class DefaultSyncView extends AbstractView implements SyncView
{
    /**
    *
    */
    private final Map<EFileSystem, AccumulativeRunnable<Object[]>> accumulatorProgressBarMinMaxText = new HashMap<>();

    /**
    *
    */
    private final Map<EFileSystem, AccumulativeRunnable<String>> accumulatorProgressBarText = new HashMap<>();

    /**
    *
    */
    private final Map<EFileSystem, AccumulativeRunnable<Integer>> accumulatorProgressBarValue = new HashMap<>();

    /**
    *
    */
    private AccumulativeRunnable<SyncPair> accumulatorTableAdd = null;

    /**
    *
    */
    private JButton buttonCompare;

    /**
    *
    */
    private JButton buttonSyncronize;

    /**
    *
    */
    private JCheckBox checkBoxChecksum;

    /**
    *
    */
    private JCheckBox checkBoxDelete;

    /**
    *
    */
    private JCheckBox checkBoxDryRun;

    /**
    *
    */
    private JCheckBox checkBoxFollowSymLinks;

    /**
    *
    */
    private JCheckBox checkBoxParallelism;

    /**
    *
    */
    private final JPanel panel;

    /**
    *
    */
    private JProgressBar progressBarFiles;

    /**
    *
    */
    private JProgressBar progressBarReceiver;

    /**
    *
    */
    private JProgressBar progressBarSender;

    /**
     *
     */
    private JTable table;

    /**
    *
    */
    private JTextField textFieldReceiverPath;

    /**
    *
    */
    private JTextField textFieldSenderPath;

    /**
     * Erstellt ein neues {@link DefaultSyncView} Object.
     */
    public DefaultSyncView()
    {
        super();

        this.panel = new JPanel();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarMinMaxText(de.freese.jsync.filesystem.EFileSystem, int, int, java.lang.String)
     */
    @Override
    public void addProgressBarMinMaxText(final EFileSystem fileSystem, final int min, final int max, final String text)
    {
        getAccumulatorProgressBarMinMaxText(fileSystem).add(new Object[]
        {
                min, max, text
        });
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarText(de.freese.jsync.filesystem.EFileSystem, java.lang.String)
     */
    @Override
    public void addProgressBarText(final EFileSystem fileSystem, final String text)
    {
        getAccumulatorProgressBarText(fileSystem).add(text);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarValue(de.freese.jsync.filesystem.EFileSystem, int)
     */
    @Override
    public void addProgressBarValue(final EFileSystem fileSystem, final int value)
    {
        getAccumulatorProgressBarValue(fileSystem).add(value);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void addSyncPair(final SyncPair syncPair)
    {
        getAccumulatorTableAdd().add(syncPair);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#clearTable()
     */
    @Override
    public void clearTable()
    {
        getTableModel().clear();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#doOnCompare(java.util.function.Consumer)
     */
    @Override
    public void doOnCompare(final Consumer<JButton> consumer)
    {
        consumer.accept(getButtonCompare());
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#doOnSyncronize(java.util.function.Consumer)
     */
    @Override
    public void doOnSyncronize(final Consumer<JButton> consumer)
    {
        consumer.accept(getButtonSyncronize());
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getOptions()
     */
    @Override
    public Options getOptions()
    {
        // @formatter:off
        return new Builder()
                .checksum(this.checkBoxChecksum.isSelected())
                .parallelism(this.checkBoxParallelism.isSelected())
                .delete(this.checkBoxDelete.isSelected())
                .followSymLinks(this.checkBoxFollowSymLinks.isSelected())
                .dryRun(this.checkBoxDryRun.isSelected())
                .build()
                ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getPanel()
     */
    @Override
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getSyncList()
     */
    @Override
    public List<SyncPair> getSyncList()
    {
        return getTableModel().getList();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getUri(de.freese.jsync.filesystem.EFileSystem)
     */
    @Override
    public URI getUri(final EFileSystem fileSystem)
    {
        // URI senderUri = URI.create(pathSender); // Erzeugt Fehler bei Leerzeichen
        // URI receiverUri = URI.create(pathReceiver);

        JTextField textField = EFileSystem.SENDER.equals(fileSystem) ? getTextFieldSenderPath() : getTextFieldReceiverPath();

        String path = textField.getText();

        if ((path == null) || path.isBlank())
        {
            return null;
        }

        URI uri = Paths.get(path).toUri();

        return uri;
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#initGUI()
     */
    @Override
    public void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());

        int row = 0;

        // Config
        JPanel configPanel = createConfigPanel();
        this.panel.add(configPanel, new GbcBuilder(0, row).gridwidth(7).anchorCenter().fillHorizontal());

        row++;
        this.panel.add(new JSeparator(), new GbcBuilder(0, row).gridwidth(7).fillHorizontal());

        // Path-Selection Sender
        row++;
        JLabel labelPath = new JLabel(getMessage("jsync.source"));
        this.panel.add(labelPath, new GbcBuilder(0, row));
        this.panel.add(getTextFieldSenderPath(), new GbcBuilder(1, row).fillHorizontal());
        JButton buttonPath = new JButton(getMessage("jsync.open"));
        buttonPath.addActionListener(event -> {
            File folder = selectFolder(getTextFieldSenderPath().getText());

            if (folder != null)
            {
                getTextFieldSenderPath().setText(folder.toString());
            }
            else
            {
                getTextFieldSenderPath().setText(null);
            }
        });
        this.panel.add(buttonPath, new GbcBuilder(2, row));

        this.panel.add(Box.createGlue(), new GbcBuilder(3, row).weightx(0.1D));

        // Path-Selection Receiver
        labelPath = new JLabel(getMessage("jsync.target"));
        this.panel.add(labelPath, new GbcBuilder(4, row).anchorEast());
        this.panel.add(getTextFieldReceiverPath(), new GbcBuilder(5, row).anchorEast().fillHorizontal());
        buttonPath = new JButton(getMessage("jsync.open"));
        buttonPath.addActionListener(event -> {
            File folder = selectFolder(getTextFieldReceiverPath().getText());

            if (folder != null)
            {
                getTextFieldReceiverPath().setText(folder.toString());
            }
            else
            {
                getTextFieldReceiverPath().setText(null);
            }
        });
        this.panel.add(buttonPath, new GbcBuilder(6, row).anchorEast());

        // Tabelle
        row++;
        JScrollPane scrollPane = new JScrollPane(getTable());
        this.panel.add(scrollPane, new GbcBuilder(0, row).gridwidth(7).fillBoth());

        // ProgressBars
        row++;
        this.panel.add(getProgressBarSender(), new GbcBuilder(0, row).gridwidth(3).fillHorizontal().insets(5, 5, 5, 0));
        this.panel.add(getProgressBarFiles(), new GbcBuilder(3, row).insets(5, 0, 5, 0));
        this.panel.add(getProgressBarReceiver(), new GbcBuilder(4, row).gridwidth(3).fillHorizontal().insets(5, 0, 5, 5));

        // Compare-Button steuern
        getTextFieldSenderPath().getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!getTextFieldSenderPath().getText().isBlank() && !getTextFieldReceiverPath().getText().isBlank());
            }
        });

        getTextFieldReceiverPath().getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!getTextFieldSenderPath().getText().isBlank() && !getTextFieldReceiverPath().getText().isBlank());
            }
        });
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarFiles(int)
     */
    @Override
    public void setProgressBarFiles(final int max)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            getProgressBarFiles().setMinimum(0);
            getProgressBarFiles().setMaximum(max);
            getProgressBarFiles().setValue(0);
            getProgressBarFiles().setString("");
        }
        else
        {
            SwingUtilities.invokeLater(() -> {
                getProgressBarFiles().setMinimum(0);
                getProgressBarFiles().setMaximum(max);
                getProgressBarFiles().setValue(0);
                getProgressBarFiles().setString("");
            });
        }
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarIndeterminate(de.freese.jsync.filesystem.EFileSystem, boolean)
     */
    @Override
    public void setProgressBarIndeterminate(final EFileSystem fileSystem, final boolean indeterminate)
    {
        final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

        if (SwingUtilities.isEventDispatchThread())
        {
            progressBar.setIndeterminate(indeterminate);
        }
        else
        {
            SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(indeterminate));
        }
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#updateLastEntry()
     */
    @Override
    public void updateLastEntry()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            getTableModel().fireTableRowsUpdated(getTableModel().getRowCount() - 1, getTableModel().getRowCount() - 1);
        }
        else
        {
            SwingUtilities.invokeLater(() -> getTableModel().fireTableRowsUpdated(getTableModel().getRowCount() - 1, getTableModel().getRowCount() - 1));
        }
    }

    /**
     * @return {@link JPanel}
     */
    private JPanel createConfigPanel()
    {
        JPanel confiPanel = new JPanel();
        confiPanel.setLayout(new GridBagLayout());
        // confiPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        // Button Compare
        confiPanel.add(getButtonCompare(), new GbcBuilder(0, 0).insets(5, 5, 5, 20));

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.options")));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0).anchorWest());

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.options.checksum"), false);
        panelOptions.add(this.checkBoxChecksum, new GbcBuilder(0, 0).anchorWest());
        this.checkBoxParallelism = new JCheckBox(getMessage("jsync.options.parallelism"), false);
        panelOptions.add(this.checkBoxParallelism, new GbcBuilder(0, 1).anchorWest());

        this.checkBoxDelete = new JCheckBox(getMessage("jsync.options.delete"), false);
        panelOptions.add(this.checkBoxDelete, new GbcBuilder(1, 0).anchorWest());
        this.checkBoxDryRun = new JCheckBox(getMessage("jsync.options.dryrun"), false);
        panelOptions.add(this.checkBoxDryRun, new GbcBuilder(1, 1).anchorWest());

        this.checkBoxFollowSymLinks = new JCheckBox(getMessage("jsync.options.followSymLinks"), true);
        panelOptions.add(this.checkBoxFollowSymLinks, new GbcBuilder(0, 2).anchorWest().gridwidth(2));

        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        // Button Synchronize
        confiPanel.add(getButtonSyncronize(), new GbcBuilder(2, 0).insets(5, 20, 5, 5));

        return confiPanel;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @return {@link AccumulativeRunnable}<Object[]>
     */
    private AccumulativeRunnable<Object[]> getAccumulatorProgressBarMinMaxText(final EFileSystem fileSystem)
    {
        AccumulativeRunnable<Object[]> accumulator = this.accumulatorProgressBarMinMaxText.get(fileSystem);

        if (accumulator == null)
        {
            ScheduledAccumulativeRunnable<Object[]> sar = new ScheduledAccumulativeRunnable<>(getScheduledExecutorService());
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            sar.doOnSubmit(chunks -> {
                Object[] chunk = chunks.get(chunks.size() - 1);
                progressBar.setMinimum((int) chunk[0]);
                progressBar.setMaximum((int) chunk[1]);
                progressBar.setString((String) chunk[2]);
            });

            this.accumulatorProgressBarMinMaxText.put(fileSystem, sar);
            accumulator = sar;
        }

        return accumulator;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @return {@link AccumulativeRunnable}<String>
     */
    private AccumulativeRunnable<String> getAccumulatorProgressBarText(final EFileSystem fileSystem)
    {
        AccumulativeRunnable<String> accumulator = this.accumulatorProgressBarText.get(fileSystem);

        if (accumulator == null)
        {
            ScheduledAccumulativeRunnable<String> sar = new ScheduledAccumulativeRunnable<>(getScheduledExecutorService());
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            sar.doOnSubmit(chunks -> progressBar.setString(chunks.get(chunks.size() - 1)));

            this.accumulatorProgressBarText.put(fileSystem, sar);
            accumulator = sar;
        }

        return accumulator;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     * @return {@link AccumulativeRunnable}<Integer>
     */
    private AccumulativeRunnable<Integer> getAccumulatorProgressBarValue(final EFileSystem fileSystem)
    {
        AccumulativeRunnable<Integer> accumulator = this.accumulatorProgressBarValue.get(fileSystem);

        if (accumulator == null)
        {
            ScheduledAccumulativeRunnable<Integer> sar = new ScheduledAccumulativeRunnable<>(getScheduledExecutorService());
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            sar.doOnSubmit(chunks -> progressBar.setValue(chunks.get(chunks.size() - 1)));

            this.accumulatorProgressBarValue.put(fileSystem, sar);
            accumulator = sar;
        }

        return accumulator;
    }

    /**
     * @return AccumulativeRunnable<SyncPair>
     */
    private AccumulativeRunnable<SyncPair> getAccumulatorTableAdd()
    {
        if (this.accumulatorTableAdd == null)
        {
            ScheduledAccumulativeRunnable<SyncPair> sar = new ScheduledAccumulativeRunnable<>(getScheduledExecutorService());
            sar.doOnSubmit(chunks -> {
                getTableModel().addAll(chunks);

                int row = getTableModel().getRowCount() - 1;
                Rectangle rectangle = getTable().getCellRect(row, 0, false);
                getTable().scrollRectToVisible(rectangle);

                int value = getTableModel().getRowCount();
                getProgressBarFiles().setValue(value);
                getProgressBarFiles().setString(getMessage("jsync.files") + ": " + value + "/" + getProgressBarFiles().getMaximum());
            });

            this.accumulatorTableAdd = sar;
        }

        return this.accumulatorTableAdd;
    }

    /**
     * @return {@link JButton}
     */
    private JButton getButtonCompare()
    {
        if (this.buttonCompare == null)
        {
            this.buttonCompare = new JButton(getMessage("jsync.compare"));
            this.buttonCompare.setEnabled(false);
        }

        return this.buttonCompare;
    }

    /**
     * @return {@link JButton}
     */
    private JButton getButtonSyncronize()
    {
        if (this.buttonSyncronize == null)
        {
            this.buttonSyncronize = new JButton(getMessage("jsync.synchronize"));
            this.buttonSyncronize.setEnabled(false);
        }

        return this.buttonSyncronize;
    }

    /**
     * @return {@link JProgressBar}
     */
    private JProgressBar getProgressBarFiles()
    {
        if (this.progressBarFiles == null)
        {
            this.progressBarFiles = new JProgressBar();
            this.progressBarFiles.setStringPainted(true);
            this.progressBarFiles.setPreferredSize(new Dimension(210, 20));
        }

        return this.progressBarFiles;
    }

    /**
     * @return {@link JProgressBar}
     */
    private JProgressBar getProgressBarReceiver()
    {
        if (this.progressBarReceiver == null)
        {
            this.progressBarReceiver = new JProgressBar();
            this.progressBarReceiver.setStringPainted(true);
        }

        return this.progressBarReceiver;
    }

    /**
     * @return {@link JProgressBar}
     */
    private JProgressBar getProgressBarSender()
    {
        if (this.progressBarSender == null)
        {
            this.progressBarSender = new JProgressBar();
            this.progressBarSender.setStringPainted(true);
        }

        return this.progressBarSender;
    }

    /**
     * @return {@link JTable}
     */
    private JTable getTable()
    {
        if (this.table == null)
        {
            this.table = new JTable();
            this.table.setModel(new SyncListTableModel());

            // Sender
            this.table.getColumnModel().getColumn(0).setPreferredWidth(1000);
            this.table.getColumnModel().getColumn(1).setMinWidth(70);
            this.table.getColumnModel().getColumn(1).setMaxWidth(70);

            // SyncStatus
            this.table.getColumnModel().getColumn(2).setMinWidth(210);
            this.table.getColumnModel().getColumn(2).setMaxWidth(210);

            // Receiver
            this.table.getColumnModel().getColumn(3).setPreferredWidth(1000);
            this.table.getColumnModel().getColumn(4).setMinWidth(70);
            this.table.getColumnModel().getColumn(4).setMaxWidth(70);

            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            this.table.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);
            this.table.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);
            this.table.getColumnModel().getColumn(4).setCellRenderer(cellRenderer);
        }

        return this.table;
    }

    /**
     * @return {@link SyncListTableModel}
     */
    private SyncListTableModel getTableModel()
    {
        return (SyncListTableModel) getTable().getModel();
    }

    /**
     * @return {@link JTextField}
     */
    private JTextField getTextFieldReceiverPath()
    {
        if (this.textFieldReceiverPath == null)
        {
            this.textFieldReceiverPath = new JTextField();
            this.textFieldReceiverPath.setEditable(true);
        }

        return this.textFieldReceiverPath;
    }

    /**
     * @return {@link JTextField}
     */
    private JTextField getTextFieldSenderPath()
    {
        if (this.textFieldSenderPath == null)
        {
            this.textFieldSenderPath = new JTextField();
            this.textFieldSenderPath.setEditable(false);
        }

        return this.textFieldSenderPath;
    }
}
