// Created: 09.08.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
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
    private final JPanel panel = new JPanel();

    /**
     * @param consumer {@link Consumer}
     */
    void doOnCompare(final Consumer<JButton> consumer)
    {
        consumer.accept(this.buttonCompare);
    }

    /**
     * @param consumer {@link Consumer}
     */
    void doOnSyncronize(final Consumer<JButton> consumer)
    {
        consumer.accept(this.buttonSyncronize);
    }

    /**
     * @return {@link JButton}
     */
    JButton getButtonCompare()
    {
        return this.buttonCompare;
    }

    /**
     * @return {@link JButton}
     */
    JButton getButtonSyncronize()
    {
        return this.buttonSyncronize;
    }

    /**
     * @see de.freese.jsync.swing.view.AbstractView#getComponent()
     */
    @Override
    Component getComponent()
    {
        return this.panel;
    }

    /**
     * @return {@link Options}
     */
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

    /**
     *
     */
    void initGUI()
    {
        this.panel.setName("confiPanel");
        this.panel.setLayout(new GridBagLayout());

        // Button Compare
        this.buttonCompare = new JButton(getMessage("jsync.compare"));
        this.buttonCompare.setName(this.panel.getName() + ".buttonCompare");
        this.buttonCompare.setEnabled(false);
        this.panel.add(this.buttonCompare, new GbcBuilder(0, 0).insets(5, 5, 5, 20));

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setName(this.panel.getName() + ".panelOptions");
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.options")));
        this.panel.add(panelOptions, new GbcBuilder(1, 0).anchorWest());

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.options.checksum"), false);
        this.checkBoxChecksum.setName(panelOptions.getName() + ".checksum");
        panelOptions.add(this.checkBoxChecksum, new GbcBuilder(0, 0).anchorWest());

        this.checkBoxDryRun = new JCheckBox(getMessage("jsync.options.dryrun"), true);
        this.checkBoxDryRun.setName(panelOptions.getName() + ".dryrun");
        panelOptions.add(this.checkBoxDryRun, new GbcBuilder(1, 0).anchorWest());

        this.checkBoxDelete = new JCheckBox(getMessage("jsync.options.delete"), false);
        this.checkBoxDelete.setName(panelOptions.getName() + ".delete");
        panelOptions.add(this.checkBoxDelete, new GbcBuilder(0, 1).anchorWest());

        this.checkBoxFollowSymLinks = new JCheckBox(getMessage("jsync.options.followSymLinks"), false);
        this.checkBoxFollowSymLinks.setName(panelOptions.getName() + ".followSymLinks");
        panelOptions.add(this.checkBoxFollowSymLinks, new GbcBuilder(1, 1).anchorWest());

        this.checkBoxParallelism = new JCheckBox(getMessage("jsync.options.parallel"), false);
        this.checkBoxParallelism.setName(panelOptions.getName() + ".parallel");
        panelOptions.add(this.checkBoxParallelism, new GbcBuilder(0, 2).gridwidth(2));

        this.panel.add(panelOptions, new GbcBuilder(1, 0));

        // Button Synchronize
        this.buttonSyncronize = new JButton(getMessage("jsync.synchronize"));
        this.buttonSyncronize.setName(this.panel.getName() + ".buttonSyncronize");
        this.buttonSyncronize.setEnabled(false);
        this.panel.add(this.buttonSyncronize, new GbcBuilder(2, 0).insets(5, 20, 5, 5));
    }
}
