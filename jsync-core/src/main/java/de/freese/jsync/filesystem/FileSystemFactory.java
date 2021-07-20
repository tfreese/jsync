// Created: 20.07.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.Objects;

import de.freese.jsync.filesystem.local.LocalhostReceiver;
import de.freese.jsync.filesystem.local.LocalhostSender;

/**
 * @author Thomas Freese
 */
public final class FileSystemFactory
{
    /**
     * @param fileSystem {@link EFileSystem}
     * @param uri {@link URI}
     *
     * @return {@link FileSystem}
     */
    public static FileSystem createFileSystem(final EFileSystem fileSystem, final URI uri)
    {
        Objects.requireNonNull(fileSystem, "fileSystem required");
        Objects.requireNonNull(uri, "uri required");

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            if ("file".equals(uri.getScheme()))
            {
                return new LocalhostSender();
            }

            throw new IllegalArgumentException("unsupported uri scheme: " + uri);
        }
        else if (EFileSystem.RECEIVER.equals(fileSystem))
        {
            if ("file".equals(uri.getScheme()))
            {
                return new LocalhostReceiver();
            }

            throw new IllegalArgumentException("unsupported uri  scheme: " + uri);
        }

        throw new IllegalArgumentException("unsupported filesystem: " + fileSystem);
    }

    /**
     * @param uri {@link URI}
     *
     * @return {@link Receiver}
     */
    public static Receiver createReceiver(final URI uri)
    {
        return (Receiver) createFileSystem(EFileSystem.RECEIVER, uri);
    }

    /**
     * @param uri {@link URI}
     *
     * @return {@link Sender}
     */
    public static Sender createSender(final URI uri)
    {
        return (Sender) createFileSystem(EFileSystem.SENDER, uri);
    }

    /**
     * Erstellt ein neues {@link FileSystemFactory} Object.
     */
    private FileSystemFactory()
    {
        super();
    }
}
