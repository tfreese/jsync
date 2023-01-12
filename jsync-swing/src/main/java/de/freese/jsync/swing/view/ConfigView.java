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
public class ConfigView extends AbstractView
{
    private final JPanel panel = new JPanel();

    private JButton buttonCompare;

    private JButton buttonSynchronize;

    private JCheckBox checkBoxChecksum;

    private JCheckBox checkBoxDelete;

    private JCheckBox checkBoxDryRun;

    private JCheckBox checkBoxFollowSymLinks;

    private JCheckBox checkBoxParallelism;

    void doOnCompare(final Consumer<JButton> consumer)
    {
        consumer.accept(this.buttonCompare);
    }

    void doOnSynchronize(final Consumer<JButton> consumer)
    {
        consumer.accept(this.buttonSynchronize);
    }

    JButton getButtonCompare()
    {
        return this.buttonCompare;
    }

    JButton getButtonSynchronize()
    {
        return this.buttonSynchronize;
    }

    /**
     * @see de.freese.jsync.swing.view.AbstractView#getComponent()
     */
    @Override
    Component getComponent()
    {
        return this.panel;
    }

    Options getOptions()
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

    void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());

        // Button Compare
        this.buttonCompare = new JButton(getMessage("jsync.compare"));
        this.buttonCompare.setEnabled(false);
        this.buttonCompare.setMinimumSize(new Dimension(150, 20));
        this.buttonCompare.setPreferredSize(new Dimension(200, 100));
        this.panel.add(this.buttonCompare, GbcBuilder.of(0, 0).insets(5, 5, 5, 20));

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.options")));
        this.panel.add(panelOptions, GbcBuilder.of(1, 0).anchorWest());

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.options.checksum"), false);
        panelOptions.add(this.checkBoxChecksum, GbcBuilder.of(0, 0).anchorWest());

        this.checkBoxDryRun = new JCheckBox(getMessage("jsync.options.dryrun"), true);
        panelOptions.add(this.checkBoxDryRun, GbcBuilder.of(1, 0).anchorWest());

        this.checkBoxDelete = new JCheckBox(getMessage("jsync.options.delete"), false);
        panelOptions.add(this.checkBoxDelete, GbcBuilder.of(0, 1).anchorWest());

        this.checkBoxFollowSymLinks = new JCheckBox(getMessage("jsync.options.followSymLinks"), false);
        panelOptions.add(this.checkBoxFollowSymLinks, GbcBuilder.of(1, 1).anchorWest());

        this.checkBoxParallelism = new JCheckBox(getMessage("jsync.options.parallel"), false);
        this.checkBoxParallelism.setName(panelOptions.getName() + ".parallel");
        panelOptions.add(this.checkBoxParallelism, GbcBuilder.of(0, 2).gridWidth(2).anchorCenter());

        this.panel.add(panelOptions, GbcBuilder.of(1, 0));

        // Button Synchronize
        this.buttonSynchronize = new JButton(getMessage("jsync.synchronize"));
        this.buttonSynchronize.setEnabled(false);
        this.buttonSynchronize.setMinimumSize(new Dimension(150, 20));
        this.buttonSynchronize.setPreferredSize(new Dimension(200, 100));
        this.panel.add(this.buttonSynchronize, GbcBuilder.of(2, 0).insets(5, 20, 5, 5));
    }
}
