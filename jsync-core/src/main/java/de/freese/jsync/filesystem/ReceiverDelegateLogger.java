// Created: 07.08.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.LongConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class ReceiverDelegateLogger implements Receiver
{
    /**
    *
    */
    private final Receiver delegate;

    /**
    *
    */
    private final Logger logger;

    /**
     * Erstellt ein neues {@link ReceiverDelegateLogger} Object.
     *
     * @param delegate {@link Receiver}
     */
    public ReceiverDelegateLogger(final Receiver delegate)
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
     * @see de.freese.jsync.filesystem.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        getLogger().info("create: {}/{}", baseDir, relativePath);

        this.delegate.createDirectory(baseDir, relativePath);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        getLogger().info("delete: {}/{}", baseDir, relativePath);

        this.delegate.delete(baseDir, relativePath, followSymLinks);
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
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        getLogger().info("update: {}/{}", baseDir, syncItem.getRelativePath());

        this.delegate.update(baseDir, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean, java.util.function.LongConsumer)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer checksumBytesReadConsumer)
    {
        getLogger().info("validate file: {}/{}, withChecksum={}", baseDir, syncItem.getRelativePath(), withChecksum);

        this.delegate.update(baseDir, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#writeFile(java.lang.String, java.lang.String, long, reactor.core.publisher.Flux)
     */
    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux)
    {
        getLogger().info("write file: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        return this.delegate.writeFile(baseDir, relativeFile, sizeOfFile, fileFlux);
    }
}
