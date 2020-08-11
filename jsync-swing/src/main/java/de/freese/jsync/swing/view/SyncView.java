// Created: 11.07.2020
package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.GbcBuilder;
import de.freese.jsync.swing.components.AbstractListTableModel;
import de.freese.jsync.swing.components.DocumentListenerAdapter;
import de.freese.jsync.swing.components.ReceiverTableModel;
import de.freese.jsync.swing.components.SenderTableModel;
import de.freese.jsync.swing.components.SyncStatusTableModel;

/**
 * @author Thomas Freese
 */
public class SyncView extends AbstractView
{
    /**
     *
     */
    private final JPanel panel;

    /**
     *
     */
    private final ReceiverView receiverView;

    /**
     *
     */
    private final SenderView senderView;

    /**
     *
     */
    private JButton buttonCompare = null;

    /**
     *
     */
    private JButton buttonSyncronize;

    /**
     *
     */
    private JCheckBox checkBoxChecksum = null;

    /**
     *
     */
    private JTable tableSyncStatus = null;

    /**
     *
     */
    private JScrollPane scrollPaneSyncStatus = null;

    /**
     * Erstellt ein neues {@link SyncView} Object.
     */
    public SyncView()
    {
        super();

        this.panel = new JPanel();
        this.senderView = new SenderView();
        this.receiverView = new ReceiverView();
    }

    /**
     * @return {@link JButton}
     */
    public JButton getButtonCompare()
    {
        return this.buttonCompare;
    }

    /**
     * @return {@link JButton}
     */
    public JButton getButtonSyncronize()
    {
        return this.buttonSyncronize;
    }

    /**
     * @return {@link JCheckBox}
     */
    public JCheckBox getCheckBoxChecksum()
    {
        return this.checkBoxChecksum;
    }

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     * @return {@link ReceiverView}
     */
    public JProgressBar getReceiverProgressBar()
    {
        return this.receiverView.getProgressBar();
    }

    /**
     * @return {@link JTable}
     */
    public JTable getReceiverTable()
    {
        return this.receiverView.getTable();
    }

    /**
     * @return {@link ReceiverTableModel}
     */
    public AbstractListTableModel<SyncPair> getReceiverTableModel()
    {
        return (ReceiverTableModel) getReceiverTable().getModel();
    }

    /**
     * @return {@link JTextField}
     */
    public JTextField getReceiverTextFieldPath()
    {
        return this.receiverView.getTextFieldPath();
    }

    /**
     * @return {@link ReceiverView}
     */
    public JProgressBar getSenderProgressBar()
    {
        return this.senderView.getProgressBar();
    }

    /**
     * @return {@link JTable}
     */
    public JTable getSenderTable()
    {
        return this.senderView.getTable();
    }

    /**
     * @return {@link SenderTableModel}
     */
    public AbstractListTableModel<SyncPair> getSenderTableModel()
    {
        return (SenderTableModel) getSenderTable().getModel();
    }

    /**
     * @return {@link JTextField}
     */
    public JTextField getSenderTextFieldPath()
    {
        return this.senderView.getTextFieldPath();
    }

    /**
     *
     */
    public void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());

        JPanel configPanel = createConfigPanel();
        this.panel.add(configPanel, new GbcBuilder(0, 0).gridwidth(3).anchorCenter().fillHorizontal());

        this.senderView.initGUI();
        this.receiverView.initGUI();

        this.panel.add(this.senderView.getPanel(), new GbcBuilder(0, 1).fillBoth());

        this.tableSyncStatus = new JTable();
        this.tableSyncStatus.setModel(new SyncStatusTableModel());
        this.scrollPaneSyncStatus = new JScrollPane(this.tableSyncStatus);
        this.scrollPaneSyncStatus.setPreferredSize(new Dimension(210, Integer.MAX_VALUE));
        this.scrollPaneSyncStatus.setMinimumSize(this.scrollPaneSyncStatus.getPreferredSize());
        this.scrollPaneSyncStatus.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.panel.add(this.scrollPaneSyncStatus, new GbcBuilder(1, 1).fillVertical().insets(5, 0, 5, 0));
        //this.panel.add(new JPanel(), new GbcBuilder(1, 1).fillVertical());

        this.panel.add(this.receiverView.getPanel(), new GbcBuilder(2, 1).fillBoth());

        // ScrollBars synchronisieren.
        JScrollBar vScrollbar = this.receiverView.getScrollPane().getVerticalScrollBar();
        this.senderView.getScrollPane().getVerticalScrollBar().setModel(vScrollbar.getModel());
        this.senderView.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.scrollPaneSyncStatus.getVerticalScrollBar().setModel(vScrollbar.getModel());

        // Buttons
        getButtonSyncronize().setEnabled(false);
        getButtonCompare().setEnabled(false);

        getSenderTextFieldPath().getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!getSenderTextFieldPath().getText().isBlank() && !getReceiverTextFieldPath().getText().isBlank());
            }
        });

        getReceiverTextFieldPath().getDocument().addDocumentListener(new DocumentListenerAdapter()
        {
            /**
             * @see DocumentListenerAdapter#insertUpdate(DocumentEvent)
             */
            @Override
            public void insertUpdate(final DocumentEvent event)
            {
                getButtonCompare().setEnabled(!getSenderTextFieldPath().getText().isBlank() && !getReceiverTextFieldPath().getText().isBlank());
            }
        });
    }

    /**
     * @return {@link JTable}
     */
    public JTable getSyncStatusTable()
    {
        return this.tableSyncStatus;
    }

    /**
     * @return {@link SyncStatusTableModel}
     */
    public SyncStatusTableModel getSyncStatusTableModel()
    {
        return (SyncStatusTableModel) this.tableSyncStatus.getModel();
    }

    /**
     * @return {@link JPanel}
     */
    private JPanel createConfigPanel()
    {
        JPanel confiPanel = new JPanel();
        confiPanel.setLayout(new GridBagLayout());
        confiPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        // Button Compare
        this.buttonCompare = new JButton(getMessage("jsync.vergleichen"));
        confiPanel.add(this.buttonCompare, new GbcBuilder(0, 0).insets(5, 5, 5, 20));
        this.buttonCompare.setEnabled(true);

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.optionen")));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.pruefsumme"), false);
        panelOptions.add(this.checkBoxChecksum, new GbcBuilder(0, 0));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        // Button Synchronize
        this.buttonSyncronize = new JButton(getMessage("jsync.synchronisieren"));
        confiPanel.add(this.buttonSyncronize, new GbcBuilder(2, 0).insets(5, 20, 5, 5));

        return confiPanel;
    }
}
