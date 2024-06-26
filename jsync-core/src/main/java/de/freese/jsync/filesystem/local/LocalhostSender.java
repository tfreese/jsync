// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import reactor.core.publisher.Flux;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.utils.ReactiveUtils;

/**
 * {@link Sender} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostSender extends AbstractLocalFileSystem implements Sender {
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile) {
        final Path path = Paths.get(baseDir, relativeFile);

        if (!Files.exists(path)) {
            final String message = String.format("file doesn't exist anymore: %s", path);
            getLogger().warn(message);

            return Flux.empty();
        }

        return ReactiveUtils.readByteChannel(() -> FileChannel.open(path, StandardOpenOption.READ));
    }
}
