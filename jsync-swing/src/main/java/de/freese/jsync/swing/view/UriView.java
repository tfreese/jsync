// Created: 25.07.2021
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.FileSystemFactory;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.swing.util.GbcBuilder;

/**
 * @author Thomas Freese
 */
class UriView extends AbstractView
{
    /**
     *
     */
    private JButton buttonOpen;
    /**
     *
     */
    private JComboBox<JSyncProtocol> comboBoxProtocol;
    /**
     *
     */
    private final JPanel panel = new JPanel();
    /**
    *
    */
    private JTextField textFieldHostPort;
    /**
     *
     */
    private JTextField textFieldPath;

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
    JComboBox<JSyncProtocol> getComboBoxProtocol()
    {
        return this.comboBoxProtocol;
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
     * @return {@link JTextField}
     */
    JTextField getTextFieldHostPort()
    {
        return this.textFieldHostPort;
    }

    /**
     * @return {@link JTextField}
     */
    JTextField getTextFieldPath()
    {
        return this.textFieldPath;
    }

    /**
     * @return {@link URI}
     */
    URI getUri()
    {
        JSyncProtocol protocol = (JSyncProtocol) this.comboBoxProtocol.getSelectedItem();
        String hostPort = this.textFieldHostPort.getText();
        String path = this.textFieldPath.getText();

        if ((path == null) || path.isBlank())
        {
            return null;
        }

        return protocol.toUri(hostPort, path);
    }

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link UriView}
     */
    UriView initGUI(final EFileSystem fileSystem)
    {
        this.panel.setLayout(new GridBagLayout());

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

        this.panel.add(labelTitle, new GbcBuilder(0, row).anchorWest());

        row++;

        this.comboBoxProtocol = new JComboBox<>();

        FileSystemFactory.getInstance().getAvailableProtocols().forEach(this.comboBoxProtocol::addItem);

        this.comboBoxProtocol.setMinimumSize(new Dimension(140, 20));
        this.comboBoxProtocol.setPreferredSize(new Dimension(140, 20));
        this.panel.add(this.comboBoxProtocol, new GbcBuilder(0, row));

        this.textFieldHostPort = new JTextField("localhost:8888");
        this.textFieldHostPort.setMinimumSize(new Dimension(100, 20));
        this.textFieldHostPort.setPreferredSize(new Dimension(100, 20));
        this.textFieldHostPort.setVisible(false);
        this.panel.add(this.textFieldHostPort, new GbcBuilder(1, row));

        this.textFieldPath = new JTextField();
        this.panel.add(this.textFieldPath, new GbcBuilder(2, row).fillHorizontal());

        this.buttonOpen = new JButton(getMessage("jsync.open"));
        this.panel.add(this.buttonOpen, new GbcBuilder(3, row));

        return this;
    }
}
