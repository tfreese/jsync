// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.LongConsumer;

import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.ReactiveUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link Sender} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostSender extends AbstractSender
{
    /**
     *
     */
    private final Generator generator;

    /**
     * Erzeugt eine neue Instanz von {@link LocalhostSender}.
     */
    public LocalhostSender()
    {
        super();

        this.generator = new DefaultGenerator();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks)
    {

        getLogger().debug("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        return this.generator.generateItems(baseDir, followSymLinks);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public Mono<String> getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        getLogger().debug("create checksum: {}/{}", baseDir, relativeFile);

        return this.generator.generateChecksum(baseDir, relativeFile, consumerBytesRead);
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        getLogger().debug("read fileHandle: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        Path path = Paths.get(baseDir, relativeFile);

        if (!Files.exists(path))
        {
            String message = String.format("file doesn't exist anymore: %s", path);
            getLogger().warn(message);
            return Flux.empty();
        }

        try
        {
            FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);

            return ReactiveUtils.readByteChannel(() -> fileChannel);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
