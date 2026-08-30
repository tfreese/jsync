package de.freese.jsync.filesystem;

import java.net.URI;

/**
 * @author Thomas Freese
 * @since 20.07.2021
 */
public interface FileSystemProvider {
    Receiver createReceiver(URI uri);

    Sender createSender(URI uri);

    boolean supportsProtocol(String scheme);
}
