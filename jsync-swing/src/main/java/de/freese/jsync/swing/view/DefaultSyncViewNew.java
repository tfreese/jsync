// Created: 09.08.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.components.SyncListTableCellRendererFileSystem;
import de.freese.jsync.swing.components.SyncListTableModel;
import de.freese.jsync.swing.components.SyncPairTableCellRendererStatus;
import de.freese.jsync.swing.components.SyncPairTableModel;
import de.freese.jsync.swing.components.SyncPairTableModelStatus;
import de.freese.jsync.swing.util.GbcBuilder;

/**
 * @author Thomas Freese
 */
public class DefaultSyncViewNew extends AbstractView implements SyncView
{
    /**
     *
     */
    private final ConfigView configView = new ConfigView();
    /**
    *
    */
    private final JPanel panel = new JPanel();
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
    private final ShowView showView = new ShowView();
    /**
    *
    */
    private UriView uriViewReceiver;
    /**
    *
    */
    private UriView uriViewSender;

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarMinMaxText(de.freese.jsync.filesystem.EFileSystem, int, int, java.lang.String)
     */
    @Override
    public void addProgressBarMinMaxText(final EFileSystem fileSystem, final int min, final int max, final String text)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarText(de.freese.jsync.filesystem.EFileSystem, java.lang.String)
     */
    @Override
    public void addProgressBarText(final EFileSystem fileSystem, final String text)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addProgressBarValue(de.freese.jsync.filesystem.EFileSystem, int)
     */
    @Override
    public void addProgressBarValue(final EFileSystem fileSystem, final int value)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#addSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void addSyncPair(final SyncPair syncPair)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#clearTable()
     */
    @Override
    public void clearTable()
    {
        // TODO Auto-generated method stub

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
     * @see de.freese.jsync.swing.view.SyncView#getSyncList()
     */
    @Override
    public List<SyncPair> getSyncList()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#getUri(de.freese.jsync.filesystem.EFileSystem)
     */
    @Override
    public URI getUri(final EFileSystem fileSystem)
    {
        // TODO Auto-generated method stub
        return null;
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
        this.configView.initGUI();
        this.configView.getComponent().setMinimumSize(new Dimension(750, 130));
        this.configView.getComponent().setPreferredSize(new Dimension(4000, 150));
        this.panel.add(this.configView.getComponent(), new GbcBuilder(0, row).anchorCenter().fillHorizontal());

        Component glue = Box.createGlue();
        glue.setMinimumSize(new Dimension(210, 1));
        glue.setPreferredSize(new Dimension(210, 1));
        glue.setMaximumSize(new Dimension(210, 1));
        this.panel.add(glue, new GbcBuilder(1, row).fillHorizontal().weightx(0));

        // Show
        this.showView.initGUI(new JTable(), new SyncListTableModel());
        this.showView.getComponent().setMinimumSize(new Dimension(750, 130));
        this.showView.getComponent().setPreferredSize(new Dimension(4000, 150));
        this.panel.add(this.showView.getComponent(), new GbcBuilder(2, row).anchorCenter().fillHorizontal());

        row++;

        // URI
        this.uriViewSender = new UriView().initGUI(EFileSystem.SENDER);
        this.panel.add(this.uriViewSender.getComponent(), new GbcBuilder(0, row).fillHorizontal());

        this.uriViewReceiver = new UriView().initGUI(EFileSystem.RECEIVER);
        this.panel.add(this.uriViewReceiver.getComponent(), new GbcBuilder(2, row).fillHorizontal());

        row++;

        // Tabelle Sender
        JTable table = new JTable();
        table.setAutoCreateRowSorter(false);
        table.setModel(new SyncPairTableModel(EFileSystem.SENDER));
        table.setDefaultRenderer(Object.class, new SyncListTableCellRendererFileSystem());
        table.getColumnModel().getColumn(0).setPreferredWidth(1000);
        table.getColumnModel().getColumn(1).setMinWidth(70);
        table.getColumnModel().getColumn(1).setMaxWidth(70);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.panel.add(scrollPane, new GbcBuilder(0, row).fillBoth());

        // Tabelle Status
        table = new JTable();
        table.setAutoCreateRowSorter(false);
        table.setModel(new SyncPairTableModelStatus());
        table.setDefaultRenderer(Object.class, new SyncPairTableCellRendererStatus());
        // table.setPreferredScrollableViewportSize(new Dimension(220, 1000));

        scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.panel.add(scrollPane, new GbcBuilder(1, row).fillBoth().weightx(0D));

        // Tabelle Receiver
        table = new JTable();
        table.setAutoCreateRowSorter(false);
        table.setModel(new SyncPairTableModel(EFileSystem.RECEIVER));
        table.setDefaultRenderer(Object.class, new SyncListTableCellRendererFileSystem());
        table.getColumnModel().getColumn(0).setPreferredWidth(1000);
        table.getColumnModel().getColumn(1).setMinWidth(70);
        table.getColumnModel().getColumn(1).setMaxWidth(70);

        scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.panel.add(scrollPane, new GbcBuilder(2, row).fillBoth());

        this.panel.add(scrollPane.getVerticalScrollBar(), new GbcBuilder(3, row).fillVertical());

        row++;

        // ProgressBars
        this.progressBarSender = new JProgressBar();
        this.progressBarSender.setStringPainted(true);

        this.progressBarFiles = new JProgressBar();
        this.progressBarFiles.setStringPainted(true);

        this.progressBarReceiver = new JProgressBar();
        this.progressBarReceiver.setStringPainted(true);

        this.panel.add(this.progressBarSender, new GbcBuilder(0, row).fillHorizontal());
        this.panel.add(this.progressBarFiles, new GbcBuilder(1, row).fillHorizontal().weightx(0D));
        this.panel.add(this.progressBarReceiver, new GbcBuilder(2, row).fillHorizontal());

        enableDebug(this.panel);
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#restoreState()
     */
    @Override
    public void restoreState()
    {
        // Path path = Paths.get(System.getProperty("user.dir"), ".jsyncGuiState");
        // Properties properties = new Properties();
        //
        // try
        // {
        // if (!Files.exists(path))
        // {
        // Files.createFile(path);
        // }
        // }
        // catch (Exception ex)
        // {
        // getLogger().error(null, ex);
        // }
        //
        // try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ))
        // {
        // properties.load(is);
        // }
        // catch (Exception ex)
        // {
        // getLogger().error(null, ex);
        // }
        //
        // getTextField(EFileSystem.SENDER).setText(properties.getProperty("sender.textfield"));
        // getTextField(EFileSystem.RECEIVER).setText(properties.getProperty("receiver.textfield"));
        //
        // getComboBox(EFileSystem.SENDER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("sender.protocol", "FILE")));
        // getComboBox(EFileSystem.RECEIVER).setSelectedItem(JSyncProtocol.valueOf(properties.getProperty("receiver.protocol", "FILE")));
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#saveState()
     */
    @Override
    public void saveState()
    {
        // Properties properties = new Properties();
        // properties.setProperty("sender.textfield", getTextField(EFileSystem.SENDER).getText());
        // properties.setProperty("receiver.textfield", getTextField(EFileSystem.RECEIVER).getText());
        //
        // properties.setProperty("sender.protocol", ((JSyncProtocol) getComboBox(EFileSystem.SENDER).getSelectedItem()).name());
        // properties.setProperty("receiver.protocol", ((JSyncProtocol) getComboBox(EFileSystem.RECEIVER).getSelectedItem()).name());
        //
        // try (OutputStream os = Files.newOutputStream(Paths.get(System.getProperty("user.dir"), ".jsyncGuiState"), StandardOpenOption.WRITE,
        // StandardOpenOption.TRUNCATE_EXISTING))
        // {
        // properties.store(os, null);
        // }
        // catch (Exception ex)
        // {
        // getLogger().error(null, ex);
        // }
    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarFilesMax(int)
     */
    @Override
    public void setProgressBarFilesMax(final int max)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#setProgressBarIndeterminate(de.freese.jsync.filesystem.EFileSystem, boolean)
     */
    @Override
    public void setProgressBarIndeterminate(final EFileSystem fileSystem, final boolean indeterminate)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.swing.view.SyncView#updateLastEntry()
     */
    @Override
    public void updateLastEntry()
    {
        // TODO Auto-generated method stub

    }
}
