// Created: 12.08.20
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;

import de.freese.jsync.Options;
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
    void addProgressBarMinMaxText(EFileSystem fileSystem, int min, int max, String text);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param text String
     */
    void addProgressBarText(EFileSystem fileSystem, String text);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param value int
     */
    void addProgressBarValue(EFileSystem fileSystem, int value);

    /**
     * @param syncPair {@link SyncPair}
     */
    void addSyncPair(SyncPair syncPair);

    /**
     *
     */
    void clearTable();

    /**
     * @param consumer {@link Consumer}
     */
    void doOnCompare(Consumer<JButton> consumer);

    /**
     * @param consumer {@link Consumer}
     */
    void doOnSyncronize(Consumer<JButton> consumer);

    /**
     * @return {@link Component}
     */
    Component getComponent();

    /**
     * @return {@link Options}
     */
    Options getOptions();

    /**
     * @return {@link List}
     */
    List<SyncPair> getSyncList();

    /**
     * @param fileSystem {@link EFileSystem}
     *
     * @return {@link URI}
     */
    URI getUri(EFileSystem fileSystem);

    /**
     * Aufbau der GUI.
     */
    void initGUI();

    /**
     *
     */
    void restoreState();

    /**
     *
     */
    void saveState();

    /**
     * @param max int
     */
    void setProgressBarFilesMax(int max);

    /**
     * @param fileSystem {@link EFileSystem}
     * @param indeterminate boolean
     */
    void setProgressBarIndeterminate(EFileSystem fileSystem, boolean indeterminate);

    /**
     *
     */
    void updateLastEntry();
}
