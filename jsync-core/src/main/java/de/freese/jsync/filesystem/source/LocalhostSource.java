// Created: 05.04.2018
package de.freese.jsync.filesystem.source;

import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import de.freese.jsync.Options;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.util.JSyncUtils;

/**
 * {@link Source} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostSource extends AbstractSource
{
    /**
    *
    */
    private final Path base;

    /**
     * Erzeugt eine neue Instanz von {@link LocalhostSource}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     */
    public LocalhostSource(final Options options, final URI baseUri)
    {
        super(options);

        Objects.requireNonNull(baseUri, "baseUri required");

        this.base = Paths.get(JSyncUtils.normalizedPath(baseUri));
    }

    /**
     * @see de.freese.jsync.filesystem.source.Source#connect()
     */
    @Override
    public void connect() throws Exception
    {
        // NO-OP
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#createSyncItems(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public Callable<Map<String, SyncItem>> createSyncItems(final GeneratorListener listener)
    {
        getLogger().debug("create SyncItems: {}", getBase().toString());

        Generator generator = new DefaultGenerator(getOptions(), getBase());
        Callable<Map<String, SyncItem>> callable = generator.createSyncItemTasks(listener);

        return callable;
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        // NO-OP
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
     * @see de.freese.jsync.filesystem.source.Source#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final FileSyncItem syncItem) throws Exception
    {
        Path path = getBase().resolve(syncItem.getRelativePath());

        getLogger().debug("get ReadableByteChannel: {}", path.toString());

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
