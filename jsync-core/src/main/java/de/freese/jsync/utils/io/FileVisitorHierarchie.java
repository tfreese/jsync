// Created: 28.07.2021
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class FileVisitorHierarchie implements FileVisitor<Path>
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVisitorHierarchie.class);
    /**
     *
     */
    private final Consumer<Path> consumer;
    /**
     *
     */
    private final PathFilter pathFilter;
    /**
     *
     */
    private final Path basePath;

    /**
     *
     */
    public FileVisitorHierarchie(Path basePath, final PathFilter pathFilter, final Consumer<Path> consumer)
    {
        super();

        this.basePath = Objects.requireNonNull(basePath, "basePath required");
        this.pathFilter = Objects.requireNonNull(pathFilter, "pathFilter required");
        this.consumer = Objects.requireNonNull(consumer, "consumer required");
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @see java.nio.file.FileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
     */
    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException
    {
        Objects.requireNonNull(dir);

        if (ex != null)
        {
            getLogger().error(dir.toString(), ex);
        }
        else if (!basePath.endsWith(dir)) // Das Basisverzeichnis wollen wir nicht.
        {
            this.consumer.accept(dir);
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
    {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);

        if (this.pathFilter.isExcludedDirectory(dir))
        {
            getLogger().debug("exclude directory: {}", dir);

            return FileVisitResult.SKIP_SUBTREE;
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
    {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        if (this.pathFilter.isExcludedFile(file))
        {
            getLogger().debug("exclude file: {}", file);
        }
        else
        {
            this.consumer.accept(file);
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.FileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
     */
    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException ex) throws IOException
    {
        Objects.requireNonNull(file);

        if (ex != null)
        {
            getLogger().error(file.toString(), ex);
        }

        return FileVisitResult.CONTINUE;
    }
}
