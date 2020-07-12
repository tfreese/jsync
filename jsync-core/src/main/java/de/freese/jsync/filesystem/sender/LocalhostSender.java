// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import de.freese.jsync.Options;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.JSyncUtils;

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
    private final Path base;

    /**
     * Erzeugt eine neue Instanz von {@link LocalhostSender}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     */
    public LocalhostSender(final Options options, final URI baseUri)
    {
        super(options);

        Objects.requireNonNull(baseUri, "baseUri required");

        this.base = Paths.get(JSyncUtils.normalizedPath(baseUri));
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
     * @see de.freese.jsync.filesystem.FileSystem#createSyncItems(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public Map<String, SyncItem> createSyncItems(final GeneratorListener listener)
    {
        getLogger().debug("create SyncItems: {}", getBase());

        Generator generator = new DefaultGenerator();
        Map<String, SyncItem> map = generator.createSyncItems(getOptions(), getBase(), listener);

        return map;
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
     * Liefert das Basis-Verzeichnis.
     *
     * @return base {@link Path}
     */
    protected Path getBase()
    {
        return this.base;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final FileSyncItem syncItem) throws Exception
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
}
