// Created: 12.08.2020
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.components.DocumentListenerAdapter;
import de.freese.jsync.swing.components.SyncListTableCellRenderer;
import de.freese.jsync.swing.components.SyncListTableModel;
import de.freese.jsync.swing.components.accumulative.AccumulativeSinkSwing;
import de.freese.jsync.swing.util.GbcBuilder;
import reactor.core.publisher.Sinks;
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
    private ConfigView configView;
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
        this.accumulatorProgressBarMinMaxText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setMinimum(value.getT1());
                progressBar.setMaximum(value.getT2());
                progressBar.setString(value.getT3());

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("addProgressBarMinMaxText - {}: {}", fileSystem, value);
                }
            });
        }).tryEmitNext(Tuples.of(min, max, text));
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarText(de.freese.jsync.filesystem.EFileSystem, java.lang.String)
     */
    @Override
    public void addProgressBarText(final EFileSystem fileSystem, final String text)
    {
        this.accumulatorProgressBarText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setString(value);

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("addProgressBarText - {}: {}", fileSystem, value);
                }
            });
        }).tryEmitNext(text);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarValue(de.freese.jsync.filesystem.EFileSystem, int)
     */
    @Override
    public void addProgressBarValue(final EFileSystem fileSystem, final int value)
    {
        this.accumulatorProgressBarValue.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(v -> {
                progressBar.setValue(v);

                if (getLogger().isDebugEnabled())
                {
                    getLogger().debug("addProgressBarValue - {}: {}", fileSystem, v);
                }
            });
        }).tryEmitNext(value);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void addSyncPair(final SyncPair syncPair)
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
                    getLogger().debug("addSyncPair - row: {}", row);
                }
            });
        }

        this.accumulatorTableAdd.tryEmitNext(syncPair);
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
        JComboBox<JSyncProtocol> comboboxSender = getComboBox(EFileSystem.SENDER);
        JComboBox<JSyncProtocol> comboboxReceiver = getComboBox(EFileSystem.RECEIVER);

        JTextField textFieldSender = getTextField(EFileSystem.SENDER);
        JTextField textFieldReceiver = getTextField(EFileSystem.RECEIVER);

        JButton buttonOpenSender = getButtonOpen(EFileSystem.SENDER);
        JButton buttonOpenReceiver = getButtonOpen(EFileSystem.RECEIVER);

        comboboxSender.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED)
            {
                return;
            }

            JSyncProtocol protocol = (JSyncProtocol) event.getItem();

            buttonOpenSender.setVisible(JSyncProtocol.FILE.equals(protocol));
        });

        comboboxReceiver.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED)
            {
                return;
            }

            JSyncProtocol protocol = (JSyncProtocol) event.getItem();

            buttonOpenReceiver.setVisible(JSyncProtocol.FILE.equals(protocol));
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
                DefaultSyncView.this.configView.getButtonCompare().setEnabled(!textFieldSender.getText().isBlank() && !textFieldReceiver.getText().isBlank());
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
                DefaultSyncView.this.configView.getButtonCompare().setEnabled(!textFieldSender.getText().isBlank() && !textFieldReceiver.getText().isBlank());
            }
        });
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#doOnCompare(java.util.function.Consumer)
     */
    @Override
    public void doOnCompare(final Consumer<JButton> consumer)
    {
        this.configView.doOnCompare(consumer);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#doOnSyncronize(java.util.function.Consumer)
     */
    @Override
    public void doOnSyncronize(final Consumer<JButton> consumer)
    {
        this.configView.doOnSyncronize(consumer);
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
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link JComboBox}
     */
    private JComboBox<JSyncProtocol> getComboBox(final EFileSystem fileSystem)
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
        return this.configView.getOptions();
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link JProgressBar}
     */
    private JProgressBar getProgressBar(final EFileSystem fileSystem)
    {
        return EFileSystem.SENDER.equals(fileSystem) ? getProgressBarSender() : getProgressBarReceiver();
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
            this.progressBarFiles.setMinimumSize(new Dimension(200, 20));
            this.progressBarFiles.setPreferredSize(this.progressBarFiles.getMinimumSize());
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
            this.progressBarReceiver.setMinimumSize(new Dimension(600, 20));
            this.progressBarReceiver.setPreferredSize(this.progressBarReceiver.getMinimumSize());
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
            this.progressBarSender.setMinimumSize(new Dimension(600, 20));
            this.progressBarSender.setPreferredSize(this.progressBarSender.getMinimumSize());
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
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getUri() : this.uriViewReceiver.getUri();
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
        this.configView = new ConfigView();
        this.configView.initGUI();
        this.panel.add(this.configView.getComponent(), new GbcBuilder(0, row).gridwidth(5).anchorCenter().fillHorizontal());

        // Show
        this.showView = new ShowView();
        this.showView.initGUI(getTable(), getTableModel());
        this.panel.add(this.showView.getComponent(), new GbcBuilder(5, row).gridwidth(5).anchorCenter().fillHorizontal());

        row++;
        this.panel.add(new JSeparator(), new GbcBuilder(0, row).gridwidth(10).fillHorizontal());

        // Path-Selection
        row++;
        this.uriViewSender = new UriView().initGUI(EFileSystem.SENDER);
        this.uriViewSender.getComponent().setMinimumSize(new Dimension(700, 50));
        this.uriViewSender.getComponent().setPreferredSize(this.uriViewSender.getComponent().getMinimumSize());
        this.panel.add(this.uriViewSender.getComponent(), new GbcBuilder(0, row).gridwidth(4).fillHorizontal());

        // this.panel.add(Box.createGlue(), new GbcBuilder(4, row).gridwidth(2).fillHorizontal());

        this.uriViewReceiver = new UriView().initGUI(EFileSystem.RECEIVER);
        this.uriViewReceiver.getComponent().setMinimumSize(new Dimension(700, 50));
        this.uriViewReceiver.getComponent().setPreferredSize(this.uriViewReceiver.getComponent().getMinimumSize());
        this.panel.add(this.uriViewReceiver.getComponent(), new GbcBuilder(6, row).gridwidth(4).fillHorizontal());

        // Tabelle
        row++;
        JScrollPane scrollPane = new JScrollPane(getTable());
        scrollPane.setName("scrollPane");
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.panel.add(scrollPane, new GbcBuilder(0, row).gridwidth(10).fillBoth());

        // ProgressBars
        row++;
        this.panel.add(getProgressBarSender(), new GbcBuilder(0, row).gridwidth(4).fillHorizontal());
        this.panel.add(getProgressBarFiles(), new GbcBuilder(4, row).gridwidth(2).fillHorizontal().insets(5, 0, 5, 0));
        this.panel.add(getProgressBarReceiver(), new GbcBuilder(6, row).gridwidth(4).fillHorizontal());

        // JPanel panelProgressBars = new JPanel();
        // panelProgressBars.setLayout(new GridBagLayout());
        // panelProgressBars.add(getProgressBarSender(), new GbcBuilder(0, 0).gridwidth(2).fillHorizontal().insets(0, 0, 0, 5));
        // panelProgressBars.add(getProgressBarFiles(), new GbcBuilder(2, 0).fillHorizontal().weightx(0.1D).insets(0, 0, 0, 0));
        // panelProgressBars.add(getProgressBarReceiver(), new GbcBuilder(3, 0).gridwidth(2).fillHorizontal().insets(0, 5, 0, 0));
        // this.panel.add(panelProgressBars, new GbcBuilder(0, row).gridwidth(10).fillHorizontal());

        // enableDebug(this.panel);
        configGui();
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#restoreState()
     */
    @Override
    public void restoreState()
    {
        Path path = Paths.get(System.getProperty("user.dir"), ".jsyncGuiState");
        Properties properties = new Properties();

        try
        {
            if (!Files.exists(path))
            {
                Files.createFile(path);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ))
        {
            properties.load(is);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        getTextField(EFileSystem.SENDER).setText(properties.getProperty("sender.textfield"));
        getTextField(EFileSystem.RECEIVER).setText(properties.getProperty("receiver.textfield"));

        getComboBox(EFileSystem.SENDER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("sender.protocol", "FILE")));
        getComboBox(EFileSystem.RECEIVER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("receiver.protocol", "FILE")));
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#saveState()
     */
    @Override
    public void saveState()
    {
        Properties properties = new Properties();
        properties.setProperty("sender.textfield", getTextField(EFileSystem.SENDER).getText());
        properties.setProperty("receiver.textfield", getTextField(EFileSystem.RECEIVER).getText());

        properties.setProperty("sender.protocol", ((JSyncProtocol) getComboBox(EFileSystem.SENDER).getSelectedItem()).name());
        properties.setProperty("receiver.protocol", ((JSyncProtocol) getComboBox(EFileSystem.RECEIVER).getSelectedItem()).name());

        try (OutputStream os = Files.newOutputStream(Paths.get(System.getProperty("user.dir"), ".jsyncGuiState"), StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING))
        {
            properties.store(os, null);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarFilesMax(int)
     */
    @Override
    public void setProgressBarFilesMax(final int max)
    {
        runInEdt(() -> {
            getProgressBarFiles().setMinimum(0);
            getProgressBarFiles().setMaximum(max);
            getProgressBarFiles().setValue(0);
            getProgressBarFiles().setString("");
        });
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarIndeterminate(de.freese.jsync.filesystem.EFileSystem, boolean)
     */
    @Override
    public void setProgressBarIndeterminate(final EFileSystem fileSystem, final boolean indeterminate)
    {
        runInEdt(() -> {
            JProgressBar progressBar = getProgressBar(fileSystem);
            progressBar.setIndeterminate(indeterminate);
        });
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#updateLastEntry()
     */
    @Override
    public void updateLastEntry()
    {
        runInEdt(() -> {
            int rowCount = getTableModel().getRowCount();
            getTableModel().fireTableRowsUpdated(rowCount - 1, rowCount - 1);
        });
    }
}
