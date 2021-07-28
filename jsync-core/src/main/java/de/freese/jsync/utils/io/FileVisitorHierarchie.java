// Created: 28.07.2021
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;

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
    final Set<Path> set;

    /**
     * Erstellt ein neues {@link FileVisitorHierarchie} Object.
     *
     * @param set {@link Set}
     */
    public FileVisitorHierarchie(final Set<Path> set)
    {
        super();

        this.set = Objects.requireNonNull(set, "set required");
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
        else
        {
            this.set.add(dir);
        }

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException
    {
        // TODO Filtern auf Verzeichnis-Ebene.
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);

        return FileVisitResult.CONTINUE;
    }

    /**
     * @see java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
    {
        // TODO Filtern auf Datei-Ebene.
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        this.set.add(file);

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
