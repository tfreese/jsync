// Created: 21.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;

import de.freese.jsync.filesystem.FileSystemProvider;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.model.JSyncProtocol;

/**
 * @author Thomas Freese
 */
public class RemoteRSocketFileSystemProvider implements FileSystemProvider {
    @Override
    public Receiver createReceiver(final URI uri) {
        return new RemoteReceiverRSocket();
    }

    @Override
    public Sender createSender(final URI uri) {
        return new RemoteSenderRSocket();
    }

    @Override
    public boolean supportsProtocol(final String scheme) {
        return JSyncProtocol.RSOCKET.getScheme().equals(scheme) || JSyncProtocol.RSOCKET_LOCAL.getScheme().equals(scheme);
    }
}
