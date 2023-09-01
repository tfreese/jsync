// Created: 21.07.2021
package de.freese.jsync.nio.filesystem;

import java.net.URI;

import de.freese.jsync.filesystem.FileSystemProvider;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.model.JSyncProtocol;

/**
 * @author Thomas Freese
 */
public class RemoteNioFileSystemProvider implements FileSystemProvider {
    @Override
    public Receiver createReceiver(final URI uri) {
        return new RemoteReceiverNio();
    }

    @Override
    public Sender createSender(final URI uri) {
        return new RemoteSenderNio();
    }

    @Override
    public boolean supportsProtocol(final String scheme) {
        return JSyncProtocol.NIO.getScheme().equals(scheme);
    }
}
