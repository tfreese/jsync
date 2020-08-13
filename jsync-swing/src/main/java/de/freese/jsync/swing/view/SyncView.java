// Created: 12.08.20
package de.freese.jsync.swing.view;

import java.net.URI;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;

/**
 * @author Thomas Freese
 */
public interface SyncView
{
    /**
     * @param fileSystem {@link EFileSystem}
     * @param min int
     * @param max int
     * @param text String
     */
    public void addProgressBarMinMaxText(EFileSystem fileSystem, int min, int max, String text);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param text String
     */
    public void addProgressBarText(EFileSystem fileSystem, String text);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param value int
     */
    public void addProgressBarValue(EFileSystem fileSystem, int value);

    /**
     * @param syncPair {@link SyncPair}
     */
    public void addSyncPair(SyncPair syncPair);

    /**
     *
     */
    public void clearTable();

    /**
     * @param consumer {@link Consumer}
     */
    public void doOnCompare(Consumer<JButton> consumer);

    /**
     * @param consumer {@link Consumer}
     */
    public void doOnSyncronize(Consumer<JButton> consumer);

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel();

    /**
     * @param fileSystem {@link EFileSystem}
     * @return {@link URI}
     */
    public URI getUri(EFileSystem fileSystem);

    /**
     *
     */
    public void initGUI();

    /**
     * @return boolean
     */
    public boolean isOptionChecksum();

    /**
     * @return boolean
     */
    public boolean isOptionParallelism();

    /**
     * @param max int
     */
    public void setProgressBarFiles(int max);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param indeterminate boolean
     */
    public void setProgressBarIndeterminate(EFileSystem fileSystem, boolean indeterminate);

    /**
     *
     */
    public void updateLastEntry();
}
