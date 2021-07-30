// Created: 12.08.2020
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.GbcBuilder;
import de.freese.jsync.swing.components.DocumentListenerAdapter;
import de.freese.jsync.swing.components.SyncListTableCellRenderer;
import de.freese.jsync.swing.components.SyncListTableModel;
import de.freese.jsync.swing.components.accumulative.AccumulativeSinkSwing;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

/**
 * @author Thomas Freese
 */
public class DefaultSyncView extends AbstractView implements SyncView
{
    /**
     *
     */
    private final Map<EFileSystem, Sinks.Many<Tuple3<Integer, Integer, String>>> accumulatorProgressBarMinMaxText = new EnumMap<>(EFileSystem.class);
    /**
     *
     */
    private final Map<EFileSystem, Sinks.Many<String>> accumulatorProgressBarText = new EnumMap<>(EFileSystem.class);
    /**
     *
     */
    private final Map<EFileSystem, Sinks.Many<Integer>> accumulatorProgressBarValue = new EnumMap<>(EFileSystem.class);
    /**
     *
     */
    private Sinks.Many<SyncPair> accumulatorTableAdd;
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
    private ShowView showView;
    /**
     *
     */
    private JTable table;
    /**
     *
     */
    private UriView uriViewReceiver;
    /**
     *
     */
    private UriView uriViewSender;

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
        getAccumulatorProgressBarMinMaxText(fileSystem).tryEmitNext(Tuples.of(min, max, text));
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarText(de.freese.jsync.filesystem.EFileSystem, java.lang.String)
     */
    @Override
    public void addProgressBarText(final EFileSystem fileSystem, final String text)
    {
        getAccumulatorProgressBarText(fileSystem).tryEmitNext(text);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarValue(de.freese.jsync.filesystem.EFileSystem, int)
     */
    @Override
    public void addProgressBarValue(final EFileSystem fileSystem, final int value)
    {
        getAccumulatorProgressBarValue(fileSystem).tryEmitNext(value);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void addSyncPair(final SyncPair syncPair)
    {
        getAccumulatorTableAdd().tryEmitNext(syncPair);
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
     * Konfiguration der GUI.
     */
    private void configGui()
    {
        JComboBox<String> comboboxSender = getComboBox(EFileSystem.SENDER);
        JComboBox<String> comboboxReceiver = getComboBox(EFileSystem.RECEIVER);

        JTextField textFieldSender = getTextField(EFileSystem.SENDER);
        JTextField textFieldReceiver = getTextField(EFileSystem.RECEIVER);

        JButton buttonOpenSender = getButtonOpen(EFileSystem.SENDER);
        JButton buttonOpenReceiver = getButtonOpen(EFileSystem.RECEIVER);

        comboboxSender.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED)
            {
                return;
            }

            String protocol = (String) event.getItem();

            buttonOpenSender.setVisible("file".equals(protocol));
        });

        comboboxReceiver.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED)
            {
                return;
            }

            String protocol = (String) event.getItem();

            buttonOpenReceiver.setVisible("file".equals(protocol));
        });

        buttonOpenSender.addActionListener(event -> {
            File folder = selectFolder(textFieldSender.getText());

            if (folder != null)
            {
                textFieldSender.setText(folder.toString());
            }
            else
            {
                textFieldSender.setText(null);
            }
        });

        buttonOpenReceiver.addActionListener(event -> {
            File folder = selectFolder(textFieldReceiver.getText());

            if (folder != null)
            {
                textFieldReceiver.setText(folder.toString());
            }
            else
            {
                textFieldReceiver.setText(null);
            }
        });

        // Compare-Button steuern
        textFieldSender.getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!textFieldSender.getText().isBlank() && !textFieldReceiver.getText().isBlank());
            }
        });

        textFieldReceiver.getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!textFieldSender.getText().isBlank() && !textFieldReceiver.getText().isBlank());
            }
        });
    }

    /**
     * @return {@link JPanel}
     */
    private JPanel createConfigPanel()
    {
        JPanel confiPanel = new JPanel();
        confiPanel.setName("confiPanel");
        confiPanel.setLayout(new GridBagLayout());

        // Button Compare
        confiPanel.add(getButtonCompare(), new GbcBuilder(0, 0).insets(5, 5, 5, 20));

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.options")));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0).anchorWest());

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.options.checksum"), false);
        this.checkBoxChecksum.setName("jsync.options.checksum");
        panelOptions.add(this.checkBoxChecksum, new GbcBuilder(0, 0).anchorWest());

        this.checkBoxDryRun = new JCheckBox(getMessage("jsync.options.dryrun"), true);
        this.checkBoxDryRun.setName("jsync.options.dryrun");
        panelOptions.add(this.checkBoxDryRun, new GbcBuilder(1, 0).anchorWest());

        this.checkBoxDelete = new JCheckBox(getMessage("jsync.options.delete"), false);
        this.checkBoxDelete.setName("jsync.options.delete");
        panelOptions.add(this.checkBoxDelete, new GbcBuilder(0, 1).anchorWest());

        this.checkBoxFollowSymLinks = new JCheckBox(getMessage("jsync.options.followSymLinks"), false);
        this.checkBoxFollowSymLinks.setName("jsync.options.followSymLinks");
        panelOptions.add(this.checkBoxFollowSymLinks, new GbcBuilder(1, 1).anchorWest());

        this.checkBoxParallelism = new JCheckBox(getMessage("jsync.options.parallel"), false);
        this.checkBoxParallelism.setName("jsync.options.parallel");
        panelOptions.add(this.checkBoxParallelism, new GbcBuilder(0, 2).gridwidth(2));

        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        // Button Synchronize
        confiPanel.add(getButtonSyncronize(), new GbcBuilder(2, 0).insets(5, 20, 5, 5));

        return confiPanel;
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
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link Many}
     */
    private Sinks.Many<Tuple3<Integer, Integer, String>> getAccumulatorProgressBarMinMaxText(final EFileSystem fileSystem)
    {
        return this.accumulatorProgressBarMinMaxText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setMinimum(value.getT1());
                progressBar.setMaximum(value.getT2());
                progressBar.setString(value.getT3());

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("getAccumulatorProgressBarMinMaxText - {}: {}", fileSystem, value);
                }
            });
        });
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link Many}
     */
    private Sinks.Many<String> getAccumulatorProgressBarText(final EFileSystem fileSystem)
    {
        return this.accumulatorProgressBarText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setString(value);

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("getAccumulatorProgressBarText - {}: {}", fileSystem, value);
                }
            });
        });
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link Many}
     */
    private Sinks.Many<Integer> getAccumulatorProgressBarValue(final EFileSystem fileSystem)
    {
        return this.accumulatorProgressBarValue.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setValue(value);

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("getAccumulatorProgressBarValue - {}: {}", fileSystem, value);
                }
            });
        });
    }

    /**
     * @return {@link Many}
     */
    private Sinks.Many<SyncPair> getAccumulatorTableAdd()
    {
        if (this.accumulatorTableAdd == null)
        {
            this.accumulatorTableAdd = new AccumulativeSinkSwing().createForList(list -> {
                getTableModel().addAll(list);

                int row = getTableModel().getRowCount() - 1;
                Rectangle rectangle = getTable().getCellRect(row, 0, false);
                getTable().scrollRectToVisible(rectangle);

                int value = getTableModel().getRowCount();
                getProgressBarFiles().setValue(value);
                getProgressBarFiles().setString(getMessage("jsync.files") + ": " + value + "/" + getProgressBarFiles().getMaximum());

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("getAccumulatorTableAdd - row: {}", row);
                }
            });
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
            this.buttonCompare.setName("buttonCompare");
            this.buttonCompare.setEnabled(false);
        }

        return this.buttonCompare;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link JButton}
     */
    private JButton getButtonOpen(final EFileSystem fileSystem)
    {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getButtonOpen() : this.uriViewReceiver.getButtonOpen();
    }

    /**
     * @return {@link JButton}
     */
    private JButton getButtonSyncronize()
    {
        if (this.buttonSyncronize == null)
        {
            this.buttonSyncronize = new JButton(getMessage("jsync.synchronize"));
            this.buttonSyncronize.setName("buttonSyncronize");
            this.buttonSyncronize.setEnabled(false);
        }

        return this.buttonSyncronize;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link JComboBox}
     */
    private JComboBox<String> getComboBox(final EFileSystem fileSystem)
    {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getComboBox() : this.uriViewReceiver.getComboBox();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getComponent()
     */
    @Override
    public Component getComponent()
    {
        return this.panel;
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
                .parallel(this.checkBoxParallelism.isSelected())
                .delete(this.checkBoxDelete.isSelected())
                .followSymLinks(this.checkBoxFollowSymLinks.isSelected())
                .dryRun(this.checkBoxDryRun.isSelected())
                .build()
                ;
        // @formatter:on
    }

    /**
     * @return {@link JProgressBar}
     */
    private JProgressBar getProgressBarFiles()
    {
        if (this.progressBarFiles == null)
        {
            this.progressBarFiles = new JProgressBar();
            this.progressBarFiles.setName("progressBarFiles");
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
            this.progressBarReceiver.setName("progressBarReceiver");
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
            this.progressBarSender.setName("progressBarSender");
            this.progressBarSender.setStringPainted(true);
        }

        return this.progressBarSender;
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getSyncList()
     */
    @Override
    public List<SyncPair> getSyncList()
    {
        Predicate<SyncPair> predicate = this.showView.getPredicate();

        return getTableModel().getStream().filter(predicate).toList();
    }

    /**
     * @return {@link JTable}
     */
    private JTable getTable()
    {
        if (this.table == null)
        {
            this.table = new JTable();
            this.table.setName("table");
            this.table.setAutoCreateRowSorter(false);
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

            this.table.setDefaultRenderer(Object.class, new SyncListTableCellRenderer());
            // DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            // cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            //
            // this.table.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);
            // this.table.getColumnModel().getColumn(2).setCellRenderer(cellRenderer);
            // this.table.getColumnModel().getColumn(4).setCellRenderer(cellRenderer);
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
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link JTextField}
     */
    private JTextField getTextField(final EFileSystem fileSystem)
    {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getTextField() : this.uriViewReceiver.getTextField();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getUri(de.freese.jsync.filesystem.EFileSystem)
     */
    @Override
    public URI getUri(final EFileSystem fileSystem)
    {
        JComboBox<String> comboBox = getComboBox(fileSystem);
        JTextField textField = getTextField(fileSystem);

        String protocol = (String) comboBox.getSelectedItem();
        String path = textField.getText();

        if ((path == null) || path.isBlank())
        {
            return null;
        }

        if ("file".equals(protocol))
        {
            return Paths.get(path).toUri();
        }
        else if ("rsocket".equals(protocol))
        {
            String[] splits = path.split("[\\/]", 2);
            String host = splits[0];
            String p = splits[1];

            return URI.create("rsocket://" + host + "/" + p.replace(" ", "%20"));
        }

        throw new IllegalStateException("unsupported protocol: " + protocol);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#initGUI()
     */
    @Override
    public void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());
        this.panel.setName("panel");

        int row = 0;

        // Config
        JPanel configPanel = createConfigPanel();
        this.panel.add(configPanel, new GbcBuilder(0, row).gridwidth(5).anchorCenter().fillHorizontal());

        // Show
        this.showView = new ShowView().initGUI(getTable(), getTableModel());
        this.panel.add(this.showView.getComponent(), new GbcBuilder(6, row).gridwidth(5).anchorCenter().fillHorizontal());

        row++;
        this.panel.add(new JSeparator(), new GbcBuilder(0, row).gridwidth(10).fillHorizontal());

        // Path-Selection
        row++;

        this.uriViewSender = new UriView().initGUI(EFileSystem.SENDER);
        this.panel.add(this.uriViewSender.getComponent(), new GbcBuilder(0, row).gridwidth(5).fillHorizontal());

        this.uriViewReceiver = new UriView().initGUI(EFileSystem.RECEIVER);
        this.panel.add(this.uriViewReceiver.getComponent(), new GbcBuilder(5, row).gridwidth(5).fillHorizontal());

        // Tabelle
        row++;
        JScrollPane scrollPane = new JScrollPane(getTable());
        scrollPane.setName("scrollPane");
        this.panel.add(scrollPane, new GbcBuilder(0, row).gridwidth(10).fillBoth());

        // ProgressBars
        row++;
        this.panel.add(getProgressBarSender(), new GbcBuilder(0, row).gridwidth(4).fillHorizontal().insets(5, 5, 5, 0));
        this.panel.add(getProgressBarFiles(), new GbcBuilder(5, row).insets(5, 0, 5, 0));
        this.panel.add(getProgressBarReceiver(), new GbcBuilder(6, row).gridwidth(4).fillHorizontal().insets(5, 0, 5, 5));

        // enableDebug(this.panel);
        configGui();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarFilesMax(int)
     */
    @Override
    public void setProgressBarFilesMax(final int max)
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
        int rowCount = getTableModel().getRowCount();

        if (SwingUtilities.isEventDispatchThread())
        {
            getTableModel().fireTableRowsUpdated(rowCount - 1, rowCount - 1);
        }
        else
        {
            SwingUtilities.invokeLater(() -> getTableModel().fireTableRowsUpdated(rowCount - 1, rowCount - 1));
        }
    }
}
