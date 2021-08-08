// Created: 25.07.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.swing.JsyncContext;
import de.freese.jsync.swing.util.GbcBuilder;

/**
 * @author Thomas Freese
 */
class UriView
{
    /**
     *
     */
    private JButton buttonOpen;

    /**
     *
     */
    private JComboBox<JSyncProtocol> comboBox;

    /**
    *
    */
    private final JPanel panel;

    /**
     *
     */
    private JTextField textField;

    /**
     * Erstellt ein neues {@link UriView} Object.
     */
    UriView()
    {
        super();

        this.panel = new JPanel();
    }

    /**
     * @return {@link JButton}
     */
    JButton getButtonOpen()
    {
        return this.buttonOpen;
    }

    /**
     * @return {@link JComboBox}<String>
     */
    JComboBox<JSyncProtocol> getComboBox()
    {
        return this.comboBox;
    }

    /**
     * @return {@link Component}
     */
    Component getComponent()
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
        return JsyncContext.getMessages().getString(key);
    }

    /**
     * @return {@link JTextField}
     */
    JTextField getTextField()
    {
        return this.textField;
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link UriView}
     */
    UriView initGUI(final EFileSystem fileSystem)
    {
        this.panel.setLayout(new GridBagLayout());
        this.panel.setName(getClass().getSimpleName() + "-" + fileSystem);

        int row = 0;

        JLabel labelTitle;

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            labelTitle = new JLabel(getMessage("jsync.source"));
        }
        else
        {
            labelTitle = new JLabel(getMessage("jsync.target"));
        }

        labelTitle.setName(this.panel.getName() + "-title");
        this.panel.add(labelTitle, new GbcBuilder(0, row).anchorWest());

        row++;

        this.comboBox = new JComboBox<>();
        this.comboBox.setName(this.panel.getName() + "-comboBox");
        this.comboBox.addItem(JSyncProtocol.FILE);
        this.comboBox.addItem(JSyncProtocol.RSOCKET);
        this.comboBox.addItem(JSyncProtocol.RSOCKET_LOCAL);
        this.comboBox.setPreferredSize(new Dimension(100, 25));
        this.panel.add(this.comboBox, new GbcBuilder(0, row));

        this.textField = new JTextField();
        this.textField.setName(this.panel.getName() + "-textField");
        this.panel.add(this.textField, new GbcBuilder(1, row).fillHorizontal());

        this.buttonOpen = new JButton(getMessage("jsync.open"));
        this.buttonOpen.setName(this.panel.getName() + "-buttonOpen");
        this.panel.add(this.buttonOpen, new GbcBuilder(2, row));

        return this;
    }
}
