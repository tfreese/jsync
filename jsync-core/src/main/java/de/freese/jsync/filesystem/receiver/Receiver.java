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
     * @param relativeDir String
     * @throws Exception Falls was schief geht.
     */
    public void deleteDirectory(String relativeDir) throws Exception;

    /**
     * Löscht eine Datei.
     *
     * @param relativeFile String
     * @throws Exception Falls was schief geht.
     */
    public void deleteFile(String relativeFile) throws Exception;

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(de.freese.jsync.model.SyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final SyncItem syncItem) throws Exception;

    /**
     * Aktualisiert ein Verzeichnis.
     *
     * @param syncItem {@link SyncItem}
     * @throws Exception Falls was schief geht.
     */
    public void updateDirectory(SyncItem syncItem) throws Exception;

    /**
     * Aktualisiert eine Datei.
     *
     * @param syncItem {@link SyncItem}
     * @throws Exception Falls was schief geht.
     */
    public void updateFile(SyncItem syncItem) throws Exception;

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     * @throws Exception Falls was schief geht.
     */
    public void validateFile(final SyncItem syncItem, boolean withChecksum) throws Exception;
}
