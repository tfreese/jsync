package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 * @since 28.07.2021
 */
public class FileVisitorDelete extends SimpleFileVisitor<Path> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVisitorDelete.class);

    @Override
    public @NonNull FileVisitResult postVisitDirectory(final @NonNull Path dir, final IOException ex) throws IOException {
        if (ex != null) {
            LOGGER.atError().log(dir.toString(), ex);
        }
        else {
            Files.delete(dir);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public @NonNull FileVisitResult visitFile(final @NonNull Path file, final @NonNull BasicFileAttributes attrs) throws IOException {
        Files.delete(file);

        return FileVisitResult.CONTINUE;
    }
}
