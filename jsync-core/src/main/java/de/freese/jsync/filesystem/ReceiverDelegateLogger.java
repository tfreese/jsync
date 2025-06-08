// Created: 07.08.2021
package de.freese.jsync.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.LongConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class ReceiverDelegateLogger implements Receiver {
    private final Receiver delegate;
    private final Logger logger;

    public ReceiverDelegateLogger(final Receiver delegate) {
        super();

        this.delegate = Objects.requireNonNull(delegate, "delegate required");

        if (this.delegate instanceof AbstractFileSystem fs) {
            this.logger = fs.getLogger();
        }
        else {
            this.logger = LoggerFactory.getLogger(this.delegate.getClass());
        }
    }

    @Override
    public void connect(final URI uri) {
        getLogger().info("connect to {}", uri);

        delegate.connect(uri);
    }

    @Override
    public void createDirectory(final String baseDir, final String relativePath) {
        getLogger().info("create: {}/{}", baseDir, relativePath);

        delegate.createDirectory(baseDir, relativePath);
    }

    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks) {
        getLogger().info("delete: {}/{}", baseDir, relativePath);

        delegate.delete(baseDir, relativePath, followSymLinks);
    }

    @Override
    public void disconnect() {
        getLogger().info("disconnect");

        delegate.disconnect();
    }

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        getLogger().info("create checksum: {}/{}", baseDir, relativeFile);

        return delegate.generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        getLogger().info("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        return delegate.generateSyncItems(baseDir, followSymLinks, pathFilter);
    }

    @Override
    public void update(final String baseDir, final SyncItem syncItem) {
        getLogger().info("update: {}/{}", baseDir, syncItem.getRelativePath());

        delegate.update(baseDir, syncItem);
    }

    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer consumerChecksumBytesRead) {
        getLogger().info("validate file: {}/{}, withChecksum={}", baseDir, syncItem.getRelativePath(), withChecksum);

        delegate.validateFile(baseDir, syncItem, withChecksum, consumerChecksumBytesRead);
    }

    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux) {
        getLogger().info("write file: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        return delegate.writeFile(baseDir, relativeFile, sizeOfFile, fileFlux);
    }

    protected Logger getLogger() {
        return logger;
    }
}
