// Created: 28.04.2020
package de.freese.jsync.filesystem.sender;

import java.nio.channels.ReadableByteChannel;

import de.freese.jsync.filesystem.FileSystem;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(String baseDir, String relativeFile);
}
