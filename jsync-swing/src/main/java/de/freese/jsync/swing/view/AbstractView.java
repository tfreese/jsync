// Created: 12.07.2020
package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.swing.JsyncContext;

/**
 * @author Thomas Freese
 */
public abstract class AbstractView
{
    /**
     * @param key String
     *
     * @return String
     */
    protected static String getMessage(final String key)
    {
        return JsyncContext.getMessages().getString(key);
    }

    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Malt einen Rahmen um jede {@link JComponent}.
     *
     * @param component {@link Component}
     */
    public void enableDebug(final Component component)
    {
        // System.out.printf("%s - %s%n", component.getName(), component.getClass().getSimpleName());

        if (component instanceof JComponent)
        {
            try
            {
                ((JComponent) component).setBorder(BorderFactory.createLineBorder(Color.MAGENTA));
            }
            catch (Exception ex)
            {
                // Ignore
            }
        }
    }

    /**
     * Malt einen Rahmen um jede {@link JComponent}.
     *
     * @param container {@link Container}
     */
    public void enableDebug(final Container container)
    {
        enableDebug((Component) container);

        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                enableDebug((Container) child);
            }
            else
            {
                enableDebug(child);
            }
        }
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
        return JsyncContext.getMainFrame();
    }

    /**
     * @return {@link ScheduledExecutorService}
     */
    protected ScheduledExecutorService getScheduledExecutorService()
    {
        return JsyncContext.getScheduledExecutorService();
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
     *
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
