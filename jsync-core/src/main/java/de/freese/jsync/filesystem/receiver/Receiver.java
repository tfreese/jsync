/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.filesystem.receiver;

import java.nio.channels.WritableByteChannel;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;

/**
 * Datensenke.
 *
 * @author Thomas Freese
 */
public interface Receiver extends FileSystem
{
    /**
     * Erstellt ein Verzeichnis.
     *
     * @param dir String
     * @throws Exception Falls was schief geht.
     */
    public void createDirectory(String dir) throws Exception;

    /**
     * Löscht ein Verzeichnis.
     *
     * @param dir String
     * @throws Exception Falls was schief geht.
     */
    public void deleteDirectory(String dir) throws Exception;

    /**
     * Löscht eine Datei.
     *
     * @param file String
     * @throws Exception Falls was schief geht.
     */
    public void deleteFile(String file) throws Exception;

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final FileSyncItem syncItem) throws Exception;

    /**
     * Aktualisiert ein Verzeichnis.
     *
     * @param syncItem {@link DirectorySyncItem}
     * @throws Exception Falls was schief geht.
     */
    public void updateDirectory(DirectorySyncItem syncItem) throws Exception;

    /**
     * Aktualisiert eine Datei.
     *
     * @param syncItem {@link FileSyncItem}
     * @throws Exception Falls was schief geht.
     */
    public void updateFile(FileSyncItem syncItem) throws Exception;

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param syncItem {@link FileSyncItem}
     * @throws Exception Falls was schief geht.
     */
    public void validateFile(final FileSyncItem syncItem) throws Exception;
}
