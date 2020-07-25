/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.filesystem.receiver;

import java.nio.channels.WritableByteChannel;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.SyncItem;

/**
 * Datensenke.
 *
 * @author Thomas Freese
 */
public interface Receiver extends FileSystem
{
    /**
     * Löscht ein Verzeichnis.
     *
     * @param baseDir String
     * @param relativeDir String
     */
    public void deleteDirectory(String baseDir, String relativeDir);

    /**
     * Löscht eine Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     */
    public void deleteFile(String baseDir, String relativeFile);

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(String baseDir, String relativeFile);

    /**
     * Aktualisiert ein Verzeichnis.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    public void updateDirectory(String baseDir, SyncItem syncItem);

    /**
     * Aktualisiert eine Datei.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    public void updateFile(String baseDir, SyncItem syncItem);

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     */
    public void validateFile(String baseDir, final SyncItem syncItem, boolean withChecksum);
}
