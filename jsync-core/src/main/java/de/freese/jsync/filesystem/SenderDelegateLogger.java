// Created: 07.08.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class SenderDelegateLogger implements Sender
{
    /**
     *
     */
    private final Sender delegate;

    /**
     *
     */
    private final Logger logger;

    /**
     * Erstellt ein neues {@link SenderDelegateLogger} Object.
     *
     * @param delegate {@link Sender}
     */
    public SenderDelegateLogger(final Sender delegate)
    {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");

        if (this.delegate instanceof AbstractFileSystem)
        {
            this.logger = ((AbstractFileSystem) this.delegate).getLogger();
        }
        else
        {
            this.logger = LoggerFactory.getLogger(this.delegate.getClass());
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        getLogger().info("connect to {}", uri);

        this.delegate.connect(uri);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        getLogger().info("disconnect");

        this.delegate.disconnect();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        getLogger().info("create checksum: {}/{}", baseDir, relativeFile);

        return this.delegate.generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks)
    {
        getLogger().info("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        return this.delegate.generateSyncItems(baseDir, followSymLinks);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumer)
    {
        getLogger().info("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        this.delegate.generateSyncItems(baseDir, followSymLinks, consumer);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        getLogger().info("read file: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        return this.delegate.readFile(baseDir, relativeFile, sizeOfFile);
    }
}
