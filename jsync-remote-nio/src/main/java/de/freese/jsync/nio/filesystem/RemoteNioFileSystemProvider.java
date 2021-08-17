// Created: 21.07.2021
package de.freese.jsync.nio.filesystem;

import java.net.URI;

import de.freese.jsync.filesystem.FileSystemProvider;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.ReceiverDelegateLogger;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filesystem.SenderDelegateLogger;

/**
 * @author Thomas Freese
 */
public class RemoteNioFileSystemProvider implements FileSystemProvider
{
    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#createReceiver(java.net.URI)
     */
    @Override
    public Receiver createReceiver(final URI uri)
    {
        return new ReceiverDelegateLogger(new RemoteReceiverNio());
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#createSender(java.net.URI)
     */
    @Override
    public Sender createSender(final URI uri)
    {
        return new SenderDelegateLogger(new RemoteSenderNio());
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystemProvider#supportsProtocol(java.net.URI)
     */
    @Override
    public boolean supportsProtocol(final URI uri)
    {
        return "nio".equals(uri.getScheme());
    }
}
