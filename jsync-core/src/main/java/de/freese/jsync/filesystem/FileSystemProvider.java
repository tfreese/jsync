// Created: 20.07.2021
package de.freese.jsync.filesystem;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface FileSystemProvider
{
    Receiver createReceiver(URI uri);

    Sender createSender(URI uri);

    boolean supportsProtocol(String scheme);
}
