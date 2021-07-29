// Created: 29.07.2021
package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.function.Predicate;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableRowSorter;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.swing.GbcBuilder;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.components.SyncListTableModel;

/**
 * @author Thomas Freese
 */
class ShowView
{
    /**
     *
     */
    private JCheckBox checkBoxDifferent;

    /**
     *
     */
    private JCheckBox checkBoxOnlyInSource;

    /**
     *
     */
    private JCheckBox checkBoxOnlyInTarget;

    /**
     *
     */
    private JCheckBox checkBoxSynchronized;

    /**
     *
     */
    private final JPanel panel;

    /**
     *
     */
    private Predicate<SyncPair> predicate = syncPair -> true;

    /**
     * Erstellt ein neues {@link ShowView} Object.
     */
    ShowView()
    {
        super();

        this.panel = new JPanel();
    }

    /**
     * @return {@link JPanel}
     */
    JPanel getComponent()
    {
        return this.panel;
    }

    /**
     * @param key String
     *
     * @return String
     */
    private String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     * @return {@link Predicate}<SyncPair>
     */
    Predicate<SyncPair> getPredicate()
    {
        return this.predicate;
    }

    /**
     * @param table {@link JTable}
     * @param tableModel {@link SyncListTableModel}
     *
     * @return {@link ShowView}
     */
    ShowView initGUI(final JTable table, final SyncListTableModel tableModel)
    {
        this.panel.setName("showPanel");
        this.panel.setLayout(new GridBagLayout());
        this.panel.setBorder(new TitledBorder(getMessage("jsync.show")));

        TableRowSorter<SyncListTableModel> rowSorter = new TableRowSorter<>(tableModel);

        this.checkBoxSynchronized = new JCheckBox(getMessage("jsync.show.synchronized"), false);
        this.checkBoxSynchronized.setName("showPanel.show.synchronized");
        this.checkBoxSynchronized.setForeground(Color.BLACK);
        this.panel.add(this.checkBoxSynchronized, new GbcBuilder(0, 0).anchorWest());
        this.checkBoxSynchronized.addItemListener(event -> {
            updatePredicate();
            rowSorter.sort();
        });

        this.checkBoxOnlyInTarget = new JCheckBox(getMessage("jsync.show.onlyInTarget"), true);
        this.checkBoxOnlyInTarget.setName("showPanel.show.onlyInTarget");
        this.checkBoxOnlyInTarget.setForeground(Color.RED);
        this.panel.add(this.checkBoxOnlyInTarget, new GbcBuilder(0, 1).anchorWest());
        this.checkBoxOnlyInTarget.addItemListener(event -> {
            updatePredicate();
            rowSorter.sort();
        });

        this.checkBoxOnlyInSource = new JCheckBox(getMessage("jsync.show.onlyInSource"), true);
        this.checkBoxOnlyInSource.setName("showPanel.show.onlyInSource");
        this.checkBoxOnlyInSource.setForeground(Color.ORANGE.darker());
        this.panel.add(this.checkBoxOnlyInSource, new GbcBuilder(0, 2).anchorWest());
        this.checkBoxOnlyInSource.addItemListener(event -> {
            updatePredicate();
            rowSorter.sort();
        });

        this.checkBoxDifferent = new JCheckBox(getMessage("jsync.show.different"), true);
        this.checkBoxDifferent.setName("showPanel.show.different");
        this.checkBoxDifferent.setForeground(Color.ORANGE.darker());
        this.panel.add(this.checkBoxDifferent, new GbcBuilder(0, 3).anchorWest());
        this.checkBoxDifferent.addItemListener(event -> {
            updatePredicate();
            rowSorter.sort();
        });

        updatePredicate();

        RowFilter<SyncListTableModel, Integer> rowFilter = new RowFilter<>()
        {
            public boolean include(final Entry<? extends SyncListTableModel, ? extends Integer> entry)
            {
                SyncPair syncPair = entry.getModel().getObjectAt(entry.getIdentifier());

                return getPredicate().test(syncPair);
            }
        };

        rowSorter.setRowFilter(rowFilter);
        table.setRowSorter(rowSorter);

        return this;
    }

    /**
     *
     */
    private void updatePredicate()
    {
        this.predicate = syncPair -> false;

        if (this.checkBoxSynchronized.isSelected())
        {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.SYNCHRONIZED.equals(syncPair.getStatus()));
        }

        if (this.checkBoxOnlyInTarget.isSelected())
        {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.ONLY_IN_TARGET.equals(syncPair.getStatus()));
        }

        if (this.checkBoxOnlyInSource.isSelected())
        {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.ONLY_IN_SOURCE.equals(syncPair.getStatus()));
        }

        if (this.checkBoxDifferent.isSelected())
        {
            this.predicate = this.predicate.or(syncPair -> syncPair.getStatus().name().startsWith("DIFFERENT"));
        }
    }
}
