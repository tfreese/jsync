// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;

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
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        getLogger().debug("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        this.generator.generateItems(baseDir, followSymLinks, consumerSyncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        getLogger().debug("get channel: {}/{}", baseDir, relativeFile);

        Path path = Paths.get(baseDir, relativeFile);

        if (!Files.exists(path))
        {
            String message = String.format("file doesn't exist anymore: %s", path);
            getLogger().warn(message);
            // getOptions().getPrintWriter().print(message);
            return null;
        }

        // FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        try
        {
            return Files.newByteChannel(path, StandardOpenOption.READ);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        getLogger().debug("create checksum: {}/{}", baseDir, relativeFile);

        String checksum = this.generator.generateChecksum(baseDir, relativeFile, consumerBytesRead);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        getLogger().debug("read chunk: {}/{}, position={}, size={}", baseDir, relativeFile, position, size);

        Path path = Paths.get(baseDir, relativeFile);

        buffer.clear();

        try
        {
            // try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r"))
            // {
            // FileChannel fileChannel = randomAccessFile.getChannel();
            try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ))
            {
                MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);

                buffer.clear();
                buffer.put(mappedByteBuffer);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
