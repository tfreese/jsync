// Created: 29.07.2021
package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.function.Predicate;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.swing.util.GbcBuilder;

/**
 * @author Thomas Freese
 */
class ShowView extends AbstractView {
    private final JPanel panel = new JPanel();
    private JCheckBox checkBoxDifferent;
    private JCheckBox checkBoxOnlyInSource;
    private JCheckBox checkBoxOnlyInTarget;
    private JCheckBox checkBoxSynchronized;
    private Predicate<SyncPair> predicate = syncPair -> true;

    /**
     * @see de.freese.jsync.swing.view.AbstractView#getComponent()
     */
    @Override
    JPanel getComponent() {
        return this.panel;
    }

    Predicate<SyncPair> getPredicate() {
        return this.predicate;
    }

    void initGUI(final TableFacade tableFacade) {
        this.panel.setLayout(new GridBagLayout());
        this.panel.setBorder(new TitledBorder(getMessage("jsync.show")));

        tableFacade.initRowSorter(this::getPredicate);

        this.checkBoxSynchronized = new JCheckBox(getMessage("jsync.show.synchronized"), true);
        this.checkBoxSynchronized.setForeground(Color.BLACK);
        this.panel.add(this.checkBoxSynchronized, GbcBuilder.of(0, 0).anchorWest());
        this.checkBoxSynchronized.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        this.checkBoxOnlyInTarget = new JCheckBox(getMessage("jsync.show.onlyInTarget"), true);
        this.checkBoxOnlyInTarget.setForeground(Color.RED);
        this.panel.add(this.checkBoxOnlyInTarget, GbcBuilder.of(0, 1).anchorWest());
        this.checkBoxOnlyInTarget.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        this.checkBoxOnlyInSource = new JCheckBox(getMessage("jsync.show.onlyInSource"), true);
        this.checkBoxOnlyInSource.setForeground(Color.ORANGE.darker());
        this.panel.add(this.checkBoxOnlyInSource, GbcBuilder.of(0, 2).anchorWest());
        this.checkBoxOnlyInSource.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        this.checkBoxDifferent = new JCheckBox(getMessage("jsync.show.different"), true);
        this.checkBoxDifferent.setForeground(Color.ORANGE.darker());
        this.panel.add(this.checkBoxDifferent, GbcBuilder.of(0, 3).anchorWest());
        this.checkBoxDifferent.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        updatePredicate();
    }

    private void updatePredicate() {
        this.predicate = syncPair -> SyncStatus.UNKNOWN.equals(syncPair.getStatus());

        if (this.checkBoxSynchronized.isSelected()) {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.SYNCHRONIZED.equals(syncPair.getStatus()));
        }

        if (this.checkBoxOnlyInTarget.isSelected()) {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.ONLY_IN_TARGET.equals(syncPair.getStatus()));
        }

        if (this.checkBoxOnlyInSource.isSelected()) {
            this.predicate = this.predicate.or(syncPair -> SyncStatus.ONLY_IN_SOURCE.equals(syncPair.getStatus()));
        }

        if (this.checkBoxDifferent.isSelected()) {
            this.predicate = this.predicate.or(syncPair -> syncPair.getStatus().name().startsWith("DIFFERENT"));
        }
    }
}
