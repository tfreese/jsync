// Created: 12.08.20
package de.freese.jsync.swing.view;

import java.awt.Component;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncPair;

/**
 * @author Thomas Freese
 */
public interface SyncView
{
    void addSyncPair(SyncPair syncPair);

    void clearTable();

    void doOnCompare(Consumer<JButton> consumer);

    void doOnSynchronize(Consumer<JButton> consumer);

    Component getComponent();

    Options getOptions();

    PathFilter getPathFilter();

    List<SyncPair> getSyncList();

    URI getUri(EFileSystem fileSystem);

    void incrementProgressBarFilesValue(int value);

    void initGui();

    void restoreState();

    void saveState();

    void setProgressBarFilesMax(int max);

    void setProgressBarIndeterminate(EFileSystem fileSystem, boolean indeterminate);

    void setProgressBarMinMaxText(EFileSystem fileSystem, int min, int max, String text);

    void setProgressBarText(EFileSystem fileSystem, String text);

    void setProgressBarValue(EFileSystem fileSystem, int value);

    void updateLastEntry();
}
