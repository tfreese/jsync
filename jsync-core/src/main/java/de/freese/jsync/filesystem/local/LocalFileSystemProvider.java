// Created: 21.07.2021
package de.freese.jsync.filesystem.local;

import java.net.URI;

import de.freese.jsync.filesystem.FileSystemProvider;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;

/**
 * @author Thomas Freese
 */
public class LocalFileSystemProvider implements FileSystemProvider
{
    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#createReceiver(java.net.URI)
     */
    @Override
    public Receiver createReceiver(final URI uri)
    {
        return new LocalhostReceiver();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#createSender(java.net.URI)
     */
    @Override
    public Sender createSender(final URI uri)
    {
        return new LocalhostSender();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#supportsProtocol(java.net.URI)
     */
    @Override
    public boolean supportsProtocol(final URI uri)
    {
        return "file".equals(uri.getScheme());
    }
}
