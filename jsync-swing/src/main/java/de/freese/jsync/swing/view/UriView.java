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
class UriView extends AbstractView {
    private final JPanel panel = new JPanel();

    private JButton buttonOpen;
    private JComboBox<JSyncProtocol> comboBoxProtocol;
    private JTextField textFieldHostPort;
    private JTextField textFieldPath;

    JButton getButtonOpen() {
        return buttonOpen;
    }

    JComboBox<JSyncProtocol> getComboBoxProtocol() {
        return comboBoxProtocol;
    }

    @Override
    Component getComponent() {
        return panel;
    }

    JTextField getTextFieldHostPort() {
        return textFieldHostPort;
    }

    JTextField getTextFieldPath() {
        return textFieldPath;
    }

    URI getUri() {
        final JSyncProtocol protocol = (JSyncProtocol) comboBoxProtocol.getSelectedItem();
        final String hostPort = textFieldHostPort.getText();
        final String path = textFieldPath.getText();

        if (path == null || path.isBlank()) {
            return null;
        }

        return protocol.toUri(hostPort, path);
    }

    UriView initGUI(final EFileSystem fileSystem) {
        panel.setLayout(new GridBagLayout());

        int row = 0;

        final JLabel labelTitle;

        if (EFileSystem.SENDER.equals(fileSystem)) {
            labelTitle = new JLabel(getMessage("jsync.source"));
        }
        else {
            labelTitle = new JLabel(getMessage("jsync.target"));
        }

        panel.add(labelTitle, GbcBuilder.of(0, row).anchorWest());

        row++;

        comboBoxProtocol = new JComboBox<>();

        FileSystemFactory.getInstance().getAvailableProtocols().forEach(comboBoxProtocol::addItem);

        comboBoxProtocol.setMinimumSize(new Dimension(140, 20));
        comboBoxProtocol.setPreferredSize(new Dimension(140, 20));
        panel.add(comboBoxProtocol, GbcBuilder.of(0, row));

        textFieldHostPort = new JTextField("localhost:8888");
        textFieldHostPort.setMinimumSize(new Dimension(100, 20));
        textFieldHostPort.setPreferredSize(new Dimension(100, 20));
        textFieldHostPort.setVisible(false);
        panel.add(textFieldHostPort, GbcBuilder.of(1, row));

        textFieldPath = new JTextField();
        panel.add(textFieldPath, GbcBuilder.of(2, row).fillHorizontal());

        buttonOpen = new JButton(getMessage("jsync.open"));
        panel.add(buttonOpen, GbcBuilder.of(3, row));

        return this;
    }
}
