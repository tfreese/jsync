// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncItemMeta;

/**
 * {@link Sender} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostSender extends AbstractSender
{
    /**
     * Erzeugt eine neue Instanz von {@link LocalhostSender}.
     *
     * @param baseUri {@link URI}
     */
    public LocalhostSender(final URI baseUri)
    {
        super(baseUri);
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
        Path path = getBase().resolve(syncItem.getRelativePath());

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
     * @see de.freese.jsync.filesystem.FileSystem#getSyncItemMeta(java.lang.String, boolean, boolean, de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public SyncItemMeta getSyncItemMeta(final String relativePath, final boolean followSymLinks, final boolean withChecksum, final GeneratorListener listener)
    {
        Generator generator = new DefaultGenerator();
        SyncItemMeta meta = generator.generateMeta(getBase().toString(), relativePath, followSymLinks, withChecksum, listener);

        return meta;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getSyncItems(boolean, de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public List<SyncItem> getSyncItems(final boolean followSymLinks, final GeneratorListener listener)
    {
        Generator generator = new DefaultGenerator();
        List<SyncItem> items = generator.generateItems(getBase().toString(), followSymLinks, listener);

        return items;
    }
}
