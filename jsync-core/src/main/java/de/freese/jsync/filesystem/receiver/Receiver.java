// Created: 28.04.2020
package de.freese.jsync.filesystem.receiver;

import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.fileHandle.FileHandle;
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
    void createDirectory(String baseDir, String relativePath);

    /**
     * Löscht ein Verzeichnis/Datei.
     *
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     */
    void delete(String baseDir, String relativePath, boolean followSymLinks);

    // /**
    // * @see de.freese.jsync.filesystem.FileSystem#getResource(java.lang.String, java.lang.String, long)
    // */
    // @Override
    // public WritableResource getResource(String baseDir, String relativeFile, long sizeOfFile);

    /**
     * Aktualisiert ein {@link SyncItem}.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    void update(String baseDir, SyncItem syncItem);

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     */
    void validateFile(String baseDir, final SyncItem syncItem, boolean withChecksum);

    // /**
    // * Schreibt nur einen bestimmten Bereich in eine Datei.
    // *
    // * @param baseDir String
    // * @param relativeFile String
    // * @param position long
    // * @param sizeOfChunk long
    // * @param byteBuffer {@link ByteBuffer}
    // */
    // public void writeChunk(String baseDir, String relativeFile, long position, final long sizeOfChunk, ByteBuffer byteBuffer);

    /**
     * Schreibt den {@link FileHandle} in die Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @param fileHandle {@link FileHandle}
     * @param bytesWrittenConsumer {@link LongConsumer}
     */
    void writeFileHandle(String baseDir, final String relativeFile, long sizeOfFile, FileHandle fileHandle, LongConsumer bytesWrittenConsumer);
}
