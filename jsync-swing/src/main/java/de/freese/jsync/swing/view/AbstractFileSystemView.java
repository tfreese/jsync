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
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import de.freese.jsync.swing.GbcBuilder;

/**
 * @author Thomas Freese
 */
public abstract class AbstractFileSystemView extends AbstractView
{
    /**
     *
     */
    private JButton buttonPath = null;

    /**
     *
     */
    private JLabel labelDirChooser = null;

    /**
     *
     */
    private final JPanel panel;

    /**
        *
        */
    private JProgressBar progressBar = null;

    /**
     *
     */
    private JScrollPane scrollPane = null;

    /**
     *
     */
    private JTable table = null;

    /**
    *
    */
    private JTextField textFieldPath = null;

    /**
     * Erstellt ein neues {@link AbstractFileSystemView} Object.
     */
    public AbstractFileSystemView()
    {
        super();

        this.panel = new JPanel();
    }

    /**
     * @return {@link JButton}
     */
    public JButton getButtonPath()
    {
        return this.buttonPath;
    }

    /**
     * @return {@link JLabel}
     */
    protected JLabel getLabelDirChooser()
    {
        return this.labelDirChooser;
    }

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     * @return {@link JProgressBar}
     */
    JProgressBar getProgressBar()
    {
        return this.progressBar;
    }

    /**
     * @return {@link JTextField}
     */
    JScrollBar getScrollBarVertical()
    {
        return this.scrollPane.getVerticalScrollBar();
    }

    /**
     * @return {@link JTable}
     */
    JTable getTable()
    {
        return this.table;
    }

    /**
     * @return {@link JTextField}
     */
    JTextField getTextFieldPath()
    {
        return this.textFieldPath;
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

        this.textFieldPath = new JTextField();
        this.textFieldPath.setEditable(false);
        this.panel.add(this.textFieldPath, new GbcBuilder(1, 0).fillHorizontal());

        this.buttonPath = new JButton(getMessage("jsync.oeffnen"));
        this.panel.add(this.buttonPath, new GbcBuilder(2, 0));

        this.buttonPath.addActionListener(event -> {

            File folder = selectFolder(this.textFieldPath.getText());

            if (folder != null)
            {
                this.textFieldPath.setText(folder.toString());
            }
            else
            {
                this.textFieldPath.setText(null);
            }
        });

        this.table = new JTable();
        this.scrollPane = new JScrollPane(this.table);
        this.panel.add(this.scrollPane, new GbcBuilder(0, 1).gridwidth(3).fillBoth());

        this.progressBar = new JProgressBar();
        this.progressBar.setStringPainted(true);
        getPanel().add(this.progressBar, new GbcBuilder(0, 2).gridwidth(3).fillHorizontal());
    }

    /**
     * @param selectedFolder String
     * @return {@link File}
     */
    protected File selectFolder(final String selectedFolder)
    {
        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setPreferredSize(new Dimension(1024, 768));
        fc.setMultiSelectionEnabled(false);
        fc.setDragEnabled(false);
        fc.setControlButtonsAreShown(true);

        File currentDirectory = null;
        File selectedDirectory = null;

        if ((selectedFolder == null) || selectedFolder.isBlank())
        {
            currentDirectory = Paths.get(System.getProperty("user.home")).toFile();
        }
        else
        {
            selectedDirectory = Paths.get(selectedFolder).toFile();
            currentDirectory = selectedDirectory.getParentFile();
        }

        fc.setCurrentDirectory(currentDirectory);
        fc.setSelectedFile(selectedDirectory);

        // UIManager.put("FileChooser.readOnly", Boolean.TRUE); // Disable NewFolderAction
        // BasicFileChooserUI ui = (BasicFileChooserUI)fc.getUI();
        // ui.getNewFolderAction().setEnabled(false);

        int choice = fc.showOpenDialog(getMainFrame());

        if (choice == JFileChooser.APPROVE_OPTION)
        {
            return fc.getSelectedFile();
        }

        return selectedDirectory;
    }
}
