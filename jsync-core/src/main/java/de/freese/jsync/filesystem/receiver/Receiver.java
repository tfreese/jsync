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
     * Löscht ein Verzeichnis/Datei.
     *
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     */
    public void delete(String baseDir, String relativePath, boolean followSymLinks);

    /**
     * Existiert ein Verzeichnis/Datei ?
     *
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     * @return boolean
     */
    public boolean exist(String baseDir, String relativePath, boolean followSymLinks);

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(String baseDir, String relativeFile);

    /**
     * Aktualisiert ein {@link SyncItem}.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    public void update(String baseDir, SyncItem syncItem);

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     */
    public void validateFile(String baseDir, final SyncItem syncItem, boolean withChecksum);
}
