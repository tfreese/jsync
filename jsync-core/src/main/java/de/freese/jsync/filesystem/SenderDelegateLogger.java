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
public class SenderDelegateLogger implements Sender {
    private final Sender delegate;
    private final Logger logger;

    public SenderDelegateLogger(final Sender delegate) {
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

        this.delegate.connect(uri);
    }

    @Override
    public void disconnect() {
        getLogger().info("disconnect");

        this.delegate.disconnect();
    }

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        getLogger().info("create checksum: {}/{}", baseDir, relativeFile);

        return this.delegate.generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        getLogger().info("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        return this.delegate.generateSyncItems(baseDir, followSymLinks, pathFilter);
    }

    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile) {
        getLogger().info("read file: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        return this.delegate.readFile(baseDir, relativeFile, sizeOfFile);
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
