// Created: 21.07.2021
package de.freese.jsync.filesystem.local;

import java.net.URI;

import de.freese.jsync.filesystem.FileSystemProvider;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.model.JSyncProtocol;

/**
 * @author Thomas Freese
 */
public class LocalFileSystemProvider implements FileSystemProvider {
    @Override
    public Receiver createReceiver(final URI uri) {
        return new LocalhostReceiver();
    }

    @Override
    public Sender createSender(final URI uri) {
        return new LocalhostSender();
    }

    @Override
    public boolean supportsProtocol(final String scheme) {
        return JSyncProtocol.FILE.getScheme().equals(scheme);
    }
}
