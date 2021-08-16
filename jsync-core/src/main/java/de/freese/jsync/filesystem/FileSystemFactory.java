// Created: 21.07.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author Thomas Freese
 */
public final class FileSystemFactory
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class FileSystemFactoryHolder
    {
        /**
         *
         */
        private static final FileSystemFactory INSTANCE = new FileSystemFactory();

        /**
         * Erstellt ein neues {@link FileSystemFactoryHolder} Object.
         */
        private FileSystemFactoryHolder()
        {
            super();
        }
    }

    /**
     * @return {@link FileSystemFactory}
     */
    public static FileSystemFactory getInstance()
    {
        return FileSystemFactoryHolder.INSTANCE;
    }

    /**
    *
    */
    private final ServiceLoader<FileSystemProvider> serviceLoader = ServiceLoader.load(FileSystemProvider.class);

    /**
     * Erstellt ein neues {@link FileSystemFactory} Object.
     */
    private FileSystemFactory()
    {
        super();
    }

    /**
     * @param uri {@link URI}
     *
     * @return {@link FileSystem}
     */
    public Receiver createReceiver(final URI uri)
    {
        Objects.requireNonNull(uri, "uri required");

        for (FileSystemProvider provider : this.serviceLoader)
        {
            if (provider.supportsProtocol(uri))
            {
                return provider.createReceiver(uri);
            }
        }

        throw new IllegalArgumentException("unsupported protocol: " + uri.getScheme());
    }

    /**
     * @param uri {@link URI}
     *
     * @return {@link FileSystem}
     */
    public Sender createSender(final URI uri)
    {
        Objects.requireNonNull(uri, "uri required");

        for (FileSystemProvider provider : this.serviceLoader)
        {
            if (provider.supportsProtocol(uri))
            {
                return provider.createSender(uri);
            }
        }

        throw new IllegalArgumentException("unsupported protocol: " + uri.getScheme());
    }
}
