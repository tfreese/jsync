// Created: 12.07.2020
package de.freese.jsync.swing.view;

import java.awt.Dimension;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.swing.JSyncSwingApplication;

/**
 * @author Thomas Freese
 */
public abstract class AbstractView
{
    /**
     * @param key String
     * @return String
     */
    protected static String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erstellt ein neues {@link AbstractView} Object.
     */
    public AbstractView()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link JFrame}
     */
    protected JFrame getMainFrame()
    {
        return JSyncSwingApplication.getInstance().getMainFrame();
    }

    /**
     * @return {@link ScheduledExecutorService}
     */
    protected ScheduledExecutorService getScheduledExecutorService()
    {
        return JSyncSwingApplication.getInstance().getScheduledExecutorService();
    }

    /**
     * @param runnable {@link Runnable}
     */
    protected void runInEdt(final Runnable runnable)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeLater(runnable);
        }
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
