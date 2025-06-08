// Created: 09.08.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.swing.util.GbcBuilder;

/**
 * @author Thomas Freese
 */
public class ConfigView extends AbstractView {
    private final JPanel panel = new JPanel();

    private JButton buttonCompare;
    private JButton buttonSynchronize;
    private JCheckBox checkBoxChecksum;
    private JCheckBox checkBoxDelete;
    private JCheckBox checkBoxDryRun;
    private JCheckBox checkBoxFollowSymLinks;
    private JCheckBox checkBoxParallelism;

    void doOnCompare(final Consumer<JButton> consumer) {
        consumer.accept(buttonCompare);
    }

    void doOnSynchronize(final Consumer<JButton> consumer) {
        consumer.accept(buttonSynchronize);
    }

    JButton getButtonCompare() {
        return buttonCompare;
    }

    JButton getButtonSynchronize() {
        return buttonSynchronize;
    }

    @Override
    Component getComponent() {
        return panel;
    }

    Options getOptions() {
        return new Builder()
                .checksum(checkBoxChecksum.isSelected())
                .parallel(checkBoxParallelism.isSelected())
                .delete(checkBoxDelete.isSelected())
                .followSymLinks(checkBoxFollowSymLinks.isSelected())
                .dryRun(checkBoxDryRun.isSelected())
                .build()
                ;
    }

    void initGUI() {
        panel.setLayout(new GridBagLayout());

        // Button Compare
        buttonCompare = new JButton(getMessage("jsync.compare"));
        buttonCompare.setEnabled(false);
        buttonCompare.setMinimumSize(new Dimension(150, 20));
        buttonCompare.setPreferredSize(new Dimension(200, 100));
        panel.add(buttonCompare, GbcBuilder.of(0, 0).insets(5, 5, 5, 20));

        // Optionen
        final JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.options")));
        panel.add(panelOptions, GbcBuilder.of(1, 0).anchorWest());

        checkBoxChecksum = new JCheckBox(getMessage("jsync.options.checksum"), false);
        panelOptions.add(checkBoxChecksum, GbcBuilder.of(0, 0).anchorWest());

        checkBoxDryRun = new JCheckBox(getMessage("jsync.options.dryrun"), true);
        panelOptions.add(checkBoxDryRun, GbcBuilder.of(1, 0).anchorWest());

        checkBoxDelete = new JCheckBox(getMessage("jsync.options.delete"), false);
        panelOptions.add(checkBoxDelete, GbcBuilder.of(0, 1).anchorWest());

        checkBoxFollowSymLinks = new JCheckBox(getMessage("jsync.options.followSymLinks"), false);
        panelOptions.add(checkBoxFollowSymLinks, GbcBuilder.of(1, 1).anchorWest());

        checkBoxParallelism = new JCheckBox(getMessage("jsync.options.parallel"), false);
        checkBoxParallelism.setName(panelOptions.getName() + ".parallel");
        panelOptions.add(checkBoxParallelism, GbcBuilder.of(0, 2).gridWidth(2).anchorCenter());

        panel.add(panelOptions, GbcBuilder.of(1, 0));

        // Button Synchronize
        buttonSynchronize = new JButton(getMessage("jsync.synchronize"));
        buttonSynchronize.setEnabled(false);
        buttonSynchronize.setMinimumSize(new Dimension(150, 20));
        buttonSynchronize.setPreferredSize(new Dimension(200, 100));
        panel.add(buttonSynchronize, GbcBuilder.of(2, 0).insets(5, 20, 5, 5));
    }
}
