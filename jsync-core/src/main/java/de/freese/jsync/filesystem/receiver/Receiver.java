// Created: 28.04.2020
package de.freese.jsync.filesystem.receiver;

import java.nio.ByteBuffer;
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
     * Erstellt ein Verzeichnis.
     *
     * @param baseDir String
     * @param relativePath String
     */
    public void createDirectory(String baseDir, String relativePath);

    /**
     * Löscht ein Verzeichnis/Datei.
     *
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     */
    public void delete(String baseDir, String relativePath, boolean followSymLinks);

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(java.lang.String, java.lang.String, long)
     */
    @Override
    public WritableByteChannel getChannel(String baseDir, String relativeFile, long size);

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

    /**
     * Schreibt nur einen bestimmten Bereich in eine Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @param buffer {@link ByteBuffer}
     */
    public void writeChunk(String baseDir, String relativeFile, long position, final long size, ByteBuffer buffer);
}
