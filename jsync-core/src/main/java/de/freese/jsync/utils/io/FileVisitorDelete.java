// Created: 28.07.2021
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class FileVisitorDelete extends SimpleFileVisitor<Path> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVisitorDelete.class);

    /**
     * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
     */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
        if (ex != null) {
            LOGGER.error(dir.toString(), ex);
        }
        else {
            Files.delete(dir);
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        Files.delete(file);

        return FileVisitResult.CONTINUE;
    }
}
