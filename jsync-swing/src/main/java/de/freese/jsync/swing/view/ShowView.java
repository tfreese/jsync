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

    @Override
    JPanel getComponent() {
        return panel;
    }

    Predicate<SyncPair> getPredicate() {
        return predicate;
    }

    void initGUI(final TableFacade tableFacade) {
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new TitledBorder(getMessage("jsync.show")));

        tableFacade.initRowSorter(this::getPredicate);

        checkBoxSynchronized = new JCheckBox(getMessage("jsync.show.synchronized"), true);
        checkBoxSynchronized.setForeground(Color.BLACK);
        panel.add(checkBoxSynchronized, GbcBuilder.of(0, 0).anchorWest());
        checkBoxSynchronized.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        checkBoxOnlyInTarget = new JCheckBox(getMessage("jsync.show.onlyInTarget"), true);
        checkBoxOnlyInTarget.setForeground(Color.RED);
        panel.add(checkBoxOnlyInTarget, GbcBuilder.of(0, 1).anchorWest());
        checkBoxOnlyInTarget.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        checkBoxOnlyInSource = new JCheckBox(getMessage("jsync.show.onlyInSource"), true);
        checkBoxOnlyInSource.setForeground(Color.ORANGE.darker());
        panel.add(checkBoxOnlyInSource, GbcBuilder.of(0, 2).anchorWest());
        checkBoxOnlyInSource.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        checkBoxDifferent = new JCheckBox(getMessage("jsync.show.different"), true);
        checkBoxDifferent.setForeground(Color.ORANGE.darker());
        panel.add(checkBoxDifferent, GbcBuilder.of(0, 3).anchorWest());
        checkBoxDifferent.addItemListener(event -> {
            updatePredicate();
            tableFacade.sort();
        });

        updatePredicate();
    }

    private void updatePredicate() {
        predicate = syncPair -> SyncStatus.UNKNOWN.equals(syncPair.getStatus());

        if (checkBoxSynchronized.isSelected()) {
            predicate = predicate.or(syncPair -> SyncStatus.SYNCHRONIZED.equals(syncPair.getStatus()));
        }

        if (checkBoxOnlyInTarget.isSelected()) {
            predicate = predicate.or(syncPair -> SyncStatus.ONLY_IN_TARGET.equals(syncPair.getStatus()));
        }

        if (checkBoxOnlyInSource.isSelected()) {
            predicate = predicate.or(syncPair -> SyncStatus.ONLY_IN_SOURCE.equals(syncPair.getStatus()));
        }

        if (checkBoxDifferent.isSelected()) {
            predicate = predicate.or(syncPair -> syncPair.getStatus().name().startsWith("DIFFERENT"));
        }
    }
}
