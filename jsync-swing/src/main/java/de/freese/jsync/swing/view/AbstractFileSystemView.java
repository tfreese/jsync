/**
 * Created: 11.07.2020
 */

package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.File;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import de.freese.jsync.swing.GbcBuilder;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.messages.Messages;

/**
 * @author Thomas Freese
 */
public abstract class AbstractFileSystemView
{
    /**
     *
     */
    private JLabel labelDirChooser = null;

    /**
     *
     */
    private final JPanel panel;

    /**
     * Erstellt ein neues {@link AbstractFileSystemView} Object.
     */
    public AbstractFileSystemView()
    {
        super();

        this.panel = new JPanel();
    }

    /**
     * @return {@link JLabel}
     */
    protected JLabel getLabelDirChooser()
    {
        return this.labelDirChooser;
    }

    /**
     * @return {@link Messages}
     */
    protected Messages getMessages()
    {
        return JSyncSwingApplication.getMessages();
    }

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     *
     */
    public void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());
        this.panel.setBorder(BorderFactory.createLineBorder(Color.RED));

        this.labelDirChooser = new JLabel("Sender/Receiver");
        this.panel.add(this.labelDirChooser, new GbcBuilder(0, 0));

        JTextField textField = new JTextField();
        this.panel.add(textField, new GbcBuilder(1, 0).fillHorizontal());

        JButton button = new JButton(getMessages().getString("jsync.oeffnen"));
        this.panel.add(button, new GbcBuilder(2, 0));

        button.addActionListener(event -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            fc.setCurrentDirectory(Paths.get(System.getProperty("user.home")).toFile());
            fc.setPreferredSize(new Dimension(1024, 768));
            fc.setMultiSelectionEnabled(false);
            fc.setDragEnabled(false);
            fc.setControlButtonsAreShown(true);

            // UIManager.put("FileChooser.readOnly", Boolean.TRUE); // Disable NewFolderAction
            // BasicFileChooserUI ui = (BasicFileChooserUI)fc.getUI();
            // ui.getNewFolderAction().setEnabled(false);

            int choice = fc.showOpenDialog(JSyncSwingApplication.getMainFrame());

            if (choice == JFileChooser.APPROVE_OPTION)
            {
                File folder = fc.getSelectedFile();

                System.out.println(folder);
            }
        });

        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        this.panel.add(scrollPane, new GbcBuilder(0, 1).gridwidth(3).fillBoth());

    }
}
