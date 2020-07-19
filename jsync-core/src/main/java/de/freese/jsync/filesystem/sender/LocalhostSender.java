// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
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
     *
     * @param baseUri {@link URI}
     */
    public LocalhostSender(final URI baseUri)
    {
        super(baseUri);

        this.generator = new DefaultGenerator();
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#connect()
     */
    @Override
    public void connect() throws Exception
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(de.freese.jsync.model.SyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final SyncItem syncItem) throws Exception
    {
        Path path = getBasePath().resolve(syncItem.getRelativePath());

        getLogger().debug("get ReadableByteChannel: {}", path);

        if (!Files.exists(path))
        {
            String message = String.format("file doesn't exist anymore: %s", path);
            getLogger().warn(message);
            // getOptions().getPrintWriter().print(message);
            return null;
        }

        return Files.newByteChannel(path, StandardOpenOption.READ);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(de.freese.jsync.model.SyncItem, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final SyncItem syncItem, final LongConsumer consumerBytesRead)
    {
        String checksum = this.generator.generateChecksum(getBasePath().toString(), syncItem.getRelativePath(), consumerBytesRead);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getSyncItems(boolean)
     */
    @Override
    public List<SyncItem> getSyncItems(final boolean followSymLinks)
    {
        List<SyncItem> items = this.generator.generateItems(getBasePath().toString(), followSymLinks);

        return items;
    }
}
