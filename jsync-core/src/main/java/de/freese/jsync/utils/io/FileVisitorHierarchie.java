package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.filter.PathFilter;

/**
 * @author Thomas Freese
 * @since 28.07.2021
 */
public class FileVisitorHierarchie implements FileVisitor<Path> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVisitorHierarchie.class);

    private final Path basePath;
    private final Consumer<Path> consumer;
    private final PathFilter pathFilter;

    public FileVisitorHierarchie(final Path basePath, final PathFilter pathFilter, final Consumer<Path> consumer) {
        super();

        this.basePath = Objects.requireNonNull(basePath, "basePath required");
        this.pathFilter = Objects.requireNonNull(pathFilter, "pathFilter required");
        this.consumer = Objects.requireNonNull(consumer, "consumer required");
    }

    @Override
    public @NonNull FileVisitResult postVisitDirectory(final @NonNull Path dir, final IOException ex) {
        Objects.requireNonNull(dir);

        if (ex != null) {
            getLogger().atError().log(dir.toString(), ex);
        }
        else if (!basePath.endsWith(dir)) {
            // We do not want the Base-Directory.
            consumer.accept(dir);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public @NonNull FileVisitResult preVisitDirectory(final @NonNull Path dir, final @NonNull BasicFileAttributes attrs) {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);

        if (pathFilter.isExcludedDirectory(dir)) {
            getLogger().debug("exclude directory: {}", dir);

            return FileVisitResult.SKIP_SUBTREE;
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public @NonNull FileVisitResult visitFile(final Path file, final @NonNull BasicFileAttributes attrs) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        if (pathFilter.isExcludedFile(file)) {
            getLogger().debug("exclude file: {}", file);
        }
        else {
            consumer.accept(file);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public @NonNull FileVisitResult visitFileFailed(final Path file, final @NonNull IOException ex) {
        Objects.requireNonNull(file);

        getLogger().atError().log(file.toString(), ex);

        return FileVisitResult.CONTINUE;
    }

    protected Logger getLogger() {
        return LOGGER;
    }
}
