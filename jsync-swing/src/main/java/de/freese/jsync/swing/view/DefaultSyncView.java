// Created: 09.08.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;

import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.components.DocumentListenerAdapter;
import de.freese.jsync.swing.components.SyncPairTableCellRendererFileSystem;
import de.freese.jsync.swing.components.SyncPairTableCellRendererStatus;
import de.freese.jsync.swing.components.accumulative.AccumulativeSinkSwing;
import de.freese.jsync.swing.util.GbcBuilder;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class DefaultSyncView extends AbstractView implements SyncView {
    private final Map<EFileSystem, Sinks.Many<Tuple3<Integer, Integer, String>>> accumulatorProgressBarMinMaxText = new EnumMap<>(EFileSystem.class);
    private final Map<EFileSystem, Sinks.Many<String>> accumulatorProgressBarText = new EnumMap<>(EFileSystem.class);
    private final Map<EFileSystem, Sinks.Many<Integer>> accumulatorProgressBarValue = new EnumMap<>(EFileSystem.class);
    private final ConfigView configView = new ConfigView();
    private final JPanel panel = new JPanel();
    private final ShowView showView = new ShowView();
    private final TableFacade tableFacade = new TableFacade();

    private Sinks.Many<Integer> accumulatorProgressFiles;
    private Sinks.Many<SyncPair> accumulatorTableAdd;
    private JProgressBar progressBarFiles;
    private JProgressBar progressBarReceiver;
    private JProgressBar progressBarSender;
    private JTextArea textAreaFilterDirs;
    private JTextArea textAreaFilterFiles;
    private UriView uriViewReceiver;
    private UriView uriViewSender;

    @Override
    public void addSyncPair(final SyncPair syncPair) {
        if (this.accumulatorTableAdd == null) {
            this.accumulatorTableAdd = new AccumulativeSinkSwing().createForList(list -> {
                this.tableFacade.addAll(list);

                this.tableFacade.scrollToLastRow();
                // this.scrollBarVertical.setValue(this.scrollBarVertical.getMaximum());

                final int rowCount = this.tableFacade.getRowCount();

                this.progressBarFiles.setValue(rowCount);
                this.progressBarFiles.setString(getMessage("jsync.files") + ": " + rowCount + "/" + this.progressBarFiles.getMaximum());

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("addSyncPair - rowCount: {}", rowCount);
                }
            });
        }

        this.accumulatorTableAdd.tryEmitNext(syncPair);
    }

    @Override
    public void clearTable() {
        this.tableFacade.clear();
    }

    @Override
    public void doOnCompare(final Consumer<JButton> consumer) {
        this.configView.doOnCompare(consumer);
    }

    @Override
    public void doOnSynchronize(final Consumer<JButton> consumer) {
        this.configView.doOnSynchronize(consumer);
    }

    @Override
    public Component getComponent() {
        return this.panel;
    }

    @Override
    public Options getOptions() {
        return this.configView.getOptions();
    }

    @Override
    public PathFilter getPathFilter() {
        final Set<String> directoryFilters = JSyncUtils.toFilter(this.textAreaFilterDirs.getText());
        final Set<String> fileFilters = JSyncUtils.toFilter(this.textAreaFilterFiles.getText());

        return new PathFilterEndsWith(directoryFilters, fileFilters);
    }

    @Override
    public List<SyncPair> getSyncList() {
        final Predicate<SyncPair> predicate = this.showView.getPredicate();

        return this.tableFacade.getStream().filter(predicate).toList();
    }

    @Override
    public URI getUri(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getUri() : this.uriViewReceiver.getUri();
    }

    @Override
    public void incrementProgressBarFilesValue(final int value) {
        if (this.accumulatorProgressFiles == null) {
            this.accumulatorProgressFiles = new AccumulativeSinkSwing().createForList(list -> {
                final int v = list.stream().mapToInt(Integer::intValue).sum();

                this.progressBarFiles.setValue(v + this.progressBarFiles.getValue());

                this.progressBarFiles.setString(getMessage("jsync.files") + ": " + v + "/" + this.progressBarFiles.getMaximum());
            });
        }

        this.accumulatorProgressFiles.tryEmitNext(value);
    }

    @Override
    public void initGui() {
        this.panel.setLayout(new GridBagLayout());
        this.panel.setName("panel");

        int row = 0;

        // Config
        this.configView.initGUI();
        this.configView.getComponent().setMinimumSize(new Dimension(750, 160));
        this.configView.getComponent().setPreferredSize(new Dimension(4000, 160));
        this.panel.add(this.configView.getComponent(), GbcBuilder.of(0, row).anchorCenter().fillHorizontal());

        // Component glue = Box.createGlue();
        // glue.setMinimumSize(new Dimension(230, 1));
        // glue.setPreferredSize(new Dimension(230, 1));
        // glue.setMaximumSize(new Dimension(230, 1));
        // this.panel.add(glue, new GbcBuilder(1, row).fillHorizontal().weightX(0));
        final JPanel panelFilter = new JPanel();
        panelFilter.setLayout(new GridBagLayout());
        panelFilter.setBorder(BorderFactory.createTitledBorder(getMessage("jsync.filter")));

        panelFilter.add(new JLabel(getMessage("jsync.directories")), GbcBuilder.of(0, 0).anchorWest());
        this.textAreaFilterDirs = new JTextArea();
        panelFilter.add(this.textAreaFilterDirs, GbcBuilder.of(0, 1).fillBoth());

        panelFilter.add(new JLabel(getMessage("jsync.files")), GbcBuilder.of(0, 2).anchorWest());
        this.textAreaFilterFiles = new JTextArea();
        panelFilter.add(this.textAreaFilterFiles, GbcBuilder.of(0, 3).fillBoth());

        panelFilter.setMinimumSize(new Dimension(230, 160));
        panelFilter.setPreferredSize(new Dimension(230, 160));
        panelFilter.setMaximumSize(new Dimension(230, 160));
        this.panel.add(panelFilter, GbcBuilder.of(1, row).fillBoth().gridHeight(2).weightX(0).weightY(0));

        // Show
        this.showView.initGUI(this.tableFacade);
        this.showView.getComponent().setMinimumSize(new Dimension(750, 160));
        this.showView.getComponent().setPreferredSize(new Dimension(4000, 160));
        this.panel.add(this.showView.getComponent(), GbcBuilder.of(2, row).anchorCenter().fillHorizontal());

        row++;

        // URI
        this.uriViewSender = new UriView().initGUI(EFileSystem.SENDER);
        this.panel.add(this.uriViewSender.getComponent(), GbcBuilder.of(0, row).fillHorizontal());

        this.uriViewReceiver = new UriView().initGUI(EFileSystem.RECEIVER);
        this.panel.add(this.uriViewReceiver.getComponent(), GbcBuilder.of(2, row).fillHorizontal());

        row++;

        // Table Sender
        final JTable tableSender = this.tableFacade.getTableSender();
        tableSender.setModel(this.tableFacade.getTableModelSender());
        tableSender.setAutoCreateRowSorter(false);
        tableSender.setDefaultRenderer(Object.class, new SyncPairTableCellRendererFileSystem());
        tableSender.getColumnModel().getColumn(0).setPreferredWidth(1000);
        tableSender.getColumnModel().getColumn(1).setMinWidth(70);
        tableSender.getColumnModel().getColumn(1).setMaxWidth(70);

        final JScrollPane scrollPaneSender = new JScrollPane(tableSender);
        scrollPaneSender.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.panel.add(scrollPaneSender, GbcBuilder.of(0, row).fillBoth());

        // Table Status
        final JTable tableStatus = this.tableFacade.getTableStatus();
        tableStatus.setModel(this.tableFacade.getTableModelStatus());
        tableStatus.setAutoCreateRowSorter(false);
        tableStatus.setDefaultRenderer(Object.class, new SyncPairTableCellRendererStatus());
        // tableStatus.setPreferredScrollableViewportSize(new Dimension(220, 1000));

        final JScrollPane scrollPaneStatus = new JScrollPane(tableStatus);
        scrollPaneStatus.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.panel.add(scrollPaneStatus, GbcBuilder.of(1, row).fillBoth().weightX(0D));

        // Table Receiver
        final JTable tableReceiver = this.tableFacade.getTableReceiver();
        tableReceiver.setModel(this.tableFacade.getTableModelReceiver());
        tableReceiver.setAutoCreateRowSorter(false);
        tableReceiver.setDefaultRenderer(Object.class, new SyncPairTableCellRendererFileSystem());
        tableReceiver.getColumnModel().getColumn(0).setPreferredWidth(1000);
        tableReceiver.getColumnModel().getColumn(1).setMinWidth(70);
        tableReceiver.getColumnModel().getColumn(1).setMaxWidth(70);

        final JScrollPane scrollPaneReceiver = new JScrollPane(tableReceiver);
        scrollPaneReceiver.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.panel.add(scrollPaneReceiver, GbcBuilder.of(2, row).fillBoth());

        // Synchronise SelectionModel
        tableReceiver.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableStatus.setSelectionModel(tableReceiver.getSelectionModel());
        tableSender.setSelectionModel(tableReceiver.getSelectionModel());

        // Synchronise Vertikale ScrollBars
        scrollPaneSender.getVerticalScrollBar().setModel(scrollPaneReceiver.getVerticalScrollBar().getModel());
        scrollPaneStatus.getVerticalScrollBar().setModel(scrollPaneReceiver.getVerticalScrollBar().getModel());

        row++;

        // ProgressBars
        this.progressBarSender = new JProgressBar();
        this.progressBarSender.setStringPainted(true);

        this.progressBarFiles = new JProgressBar();
        this.progressBarFiles.setStringPainted(true);

        this.progressBarReceiver = new JProgressBar();
        this.progressBarReceiver.setStringPainted(true);

        this.panel.add(this.progressBarSender, GbcBuilder.of(0, row).fillHorizontal());
        this.panel.add(this.progressBarFiles, GbcBuilder.of(1, row).fillHorizontal().weightX(0D));
        this.panel.add(this.progressBarReceiver, GbcBuilder.of(2, row).fillHorizontal());

        configGui();
        // enableDebug(this.panel);
    }

    @Override
    public void restoreState() {
        final Path path = Paths.get(System.getProperty("user.home"), ".java-apps", "jsync", "jSyncGuiState");
        final Properties properties = new Properties();

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
                properties.load(is);
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }

        getTextFieldHostPort(EFileSystem.SENDER).setText(properties.getProperty("sender.textfieldHostPort"));
        getTextFieldHostPort(EFileSystem.RECEIVER).setText(properties.getProperty("receiver.textfieldHostPort"));

        getTextFieldPath(EFileSystem.SENDER).setText(properties.getProperty("sender.textfieldPath"));
        getTextFieldPath(EFileSystem.RECEIVER).setText(properties.getProperty("receiver.textfieldPath"));

        getComboBoxProtocol(EFileSystem.SENDER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("sender.protocol", "FILE")));
        getComboBoxProtocol(EFileSystem.RECEIVER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("receiver.protocol", "FILE")));

        this.textAreaFilterDirs.setText(properties.getProperty("filter.directories", "target; build; .settings; .idea; .gradle"));
        this.textAreaFilterFiles.setText(properties.getProperty("filter.files", ".class; .log"));
    }

    @Override
    public void saveState() {
        final Path path = Paths.get(System.getProperty("user.home"), ".java-apps", "jsync", "jSyncGuiState");

        final Properties properties = new Properties();
        properties.setProperty("sender.textfieldHostPort", getTextFieldHostPort(EFileSystem.SENDER).getText());
        properties.setProperty("receiver.textfieldHostPort", getTextFieldHostPort(EFileSystem.RECEIVER).getText());

        properties.setProperty("sender.textfieldPath", getTextFieldPath(EFileSystem.SENDER).getText());
        properties.setProperty("receiver.textfieldPath", getTextFieldPath(EFileSystem.RECEIVER).getText());

        properties.setProperty("sender.protocol", ((JSyncProtocol) getComboBoxProtocol(EFileSystem.SENDER).getSelectedItem()).name());
        properties.setProperty("receiver.protocol", ((JSyncProtocol) getComboBoxProtocol(EFileSystem.RECEIVER).getSelectedItem()).name());

        properties.setProperty("filter.directories", this.textAreaFilterDirs.getText());
        properties.setProperty("filter.files", this.textAreaFilterFiles.getText());

        try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            properties.store(os, null);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    @Override
    public void setProgressBarFilesMax(final int max) {
        runInEdt(() -> {
            this.progressBarFiles.setMinimum(0);
            this.progressBarFiles.setMaximum(max);
            this.progressBarFiles.setValue(0);
            this.progressBarFiles.setString("");
        });
    }

    @Override
    public void setProgressBarIndeterminate(final EFileSystem fileSystem, final boolean indeterminate) {
        runInEdt(() -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);
            progressBar.setIndeterminate(indeterminate);
        });
    }

    @Override
    public void setProgressBarMinMaxText(final EFileSystem fileSystem, final int min, final int max, final String text) {
        this.accumulatorProgressBarMinMaxText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setMinimum(value.getT1());
                progressBar.setMaximum(value.getT2());
                progressBar.setString(value.getT3());

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("addProgressBarMinMaxText - {}: {}", fileSystem, value);
                }
            });
        }).tryEmitNext(Tuples.of(min, max, text));
    }

    @Override
    public void setProgressBarText(final EFileSystem fileSystem, final String text) {
        this.accumulatorProgressBarText.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(value -> {
                progressBar.setString(value);

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("addProgressBarText - {}: {}", fileSystem, value);
                }
            });
        }).tryEmitNext(text);
    }

    @Override
    public void setProgressBarValue(final EFileSystem fileSystem, final int value) {
        this.accumulatorProgressBarValue.computeIfAbsent(fileSystem, key -> {
            final JProgressBar progressBar = getProgressBar(fileSystem);

            return new AccumulativeSinkSwing().createForSingle(v -> {
                progressBar.setValue(v);

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("addProgressBarValue - {}: {}", fileSystem, v);
                }
            });
        }).tryEmitNext(value);
    }

    @Override
    public void updateLastEntry() {
        runInEdt(() -> {
            final int rowCount = this.tableFacade.getRowCount();
            this.tableFacade.fireTableRowsUpdated(rowCount - 1, rowCount - 1);
        });
    }

    protected File selectFolder(final String selectedFolder) {
        final JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setPreferredSize(new Dimension(1024, 768));
        fc.setMultiSelectionEnabled(false);
        fc.setDragEnabled(false);
        fc.setControlButtonsAreShown(true);

        File currentDirectory = null;
        File selectedDirectory = null;

        if (selectedFolder == null || selectedFolder.isBlank()) {
            currentDirectory = Paths.get(System.getProperty("user.home")).toFile();
        }
        else {
            selectedDirectory = Paths.get(selectedFolder).toFile();
            currentDirectory = selectedDirectory.getParentFile();
        }

        fc.setCurrentDirectory(currentDirectory);
        fc.setSelectedFile(selectedDirectory);

        // UIManager.put("FileChooser.readOnly", Boolean.TRUE); // Disable NewFolderAction
        // BasicFileChooserUI ui = (BasicFileChooserUI)fc.getUI();
        // ui.getNewFolderAction().setEnabled(false);

        final int choice = fc.showOpenDialog(getMainFrame());

        if (choice == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }

        return selectedDirectory;
    }

    private void configGui() {
        final JComboBox<JSyncProtocol> comboBoxProtocolSender = getComboBoxProtocol(EFileSystem.SENDER);
        final JComboBox<JSyncProtocol> comboBoxProtocolReceiver = getComboBoxProtocol(EFileSystem.RECEIVER);

        final JTextField textFieldHostPortSender = getTextFieldHostPort(EFileSystem.SENDER);
        final JTextField textFieldHostPortReceiver = getTextFieldHostPort(EFileSystem.RECEIVER);

        final JTextField textFieldPathSender = getTextFieldPath(EFileSystem.SENDER);
        final JTextField textFieldPathReceiver = getTextFieldPath(EFileSystem.RECEIVER);

        final JButton buttonOpenSender = getButtonOpen(EFileSystem.SENDER);
        final JButton buttonOpenReceiver = getButtonOpen(EFileSystem.RECEIVER);

        comboBoxProtocolSender.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED) {
                return;
            }

            final JSyncProtocol protocol = (JSyncProtocol) event.getItem();

            textFieldHostPortSender.setVisible(protocol.isRemote());
            buttonOpenSender.setVisible(JSyncProtocol.FILE.equals(protocol));

            this.uriViewSender.getComponent().revalidate();
            this.uriViewSender.getComponent().repaint();
        });

        comboBoxProtocolReceiver.addItemListener(event -> {
            if (event.getStateChange() != ItemEvent.SELECTED) {
                return;
            }

            final JSyncProtocol protocol = (JSyncProtocol) event.getItem();

            textFieldHostPortReceiver.setVisible(protocol.isRemote());
            buttonOpenReceiver.setVisible(JSyncProtocol.FILE.equals(protocol));

            this.uriViewReceiver.getComponent().revalidate();
            this.uriViewReceiver.getComponent().repaint();
        });

        buttonOpenSender.addActionListener(event -> {
            final File folder = selectFolder(textFieldPathSender.getText());

            if (folder != null) {
                textFieldPathSender.setText(folder.toString());
            }
            else {
                textFieldPathSender.setText(null);
            }
        });

        buttonOpenReceiver.addActionListener(event -> {
            final File folder = selectFolder(textFieldPathReceiver.getText());

            if (folder != null) {
                textFieldPathReceiver.setText(folder.toString());
            }
            else {
                textFieldPathReceiver.setText(null);
            }
        });

        // Compare-Button
        textFieldPathSender.getDocument().addDocumentListener(new DocumentListenerAdapter() {
            @Override
            public void insertUpdate(final DocumentEvent event) {
                DefaultSyncView.this.configView.getButtonCompare().setEnabled(!textFieldPathSender.getText().isBlank() && !textFieldPathReceiver.getText().isBlank());
            }
        });

        textFieldPathReceiver.getDocument().addDocumentListener(new DocumentListenerAdapter() {
            @Override
            public void insertUpdate(final DocumentEvent event) {
                DefaultSyncView.this.configView.getButtonCompare().setEnabled(!textFieldPathSender.getText().isBlank() && !textFieldPathReceiver.getText().isBlank());
            }
        });
    }

    private JButton getButtonOpen(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getButtonOpen() : this.uriViewReceiver.getButtonOpen();
    }

    private JComboBox<JSyncProtocol> getComboBoxProtocol(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getComboBoxProtocol() : this.uriViewReceiver.getComboBoxProtocol();
    }

    private JProgressBar getProgressBar(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.progressBarSender : this.progressBarReceiver;
    }

    private JTextField getTextFieldHostPort(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getTextFieldHostPort() : this.uriViewReceiver.getTextFieldHostPort();
    }

    private JTextField getTextFieldPath(final EFileSystem fileSystem) {
        return EFileSystem.SENDER.equals(fileSystem) ? this.uriViewSender.getTextFieldPath() : this.uriViewReceiver.getTextFieldPath();
    }
}
