// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.utils.ReactiveUtils;
import reactor.core.publisher.Flux;

/**
 * {@link Sender} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostSender extends AbstractLocalFileSystem implements Sender
{
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
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
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
