// Created: 20.07.2021
package de.freese.jsync.filesystem;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface FileSystemProvider
{
    /**
     * @param uri {@link URI}
     *
     * @return {@link FileSystem}
     */
    Receiver createReceiver(final URI uri);

    /**
     * @param uri {@link URI}
     *
     * @return {@link FileSystem}
     */
    Sender createSender(final URI uri);

    /**
     * @param scheme {@link String}
     *
     * @return boolean
     */
    boolean supportsProtocol(String scheme);
}
