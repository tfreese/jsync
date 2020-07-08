// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import de.freese.jsync.Options;

/**
 * Basis-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractGenerator implements Generator
{
    /**
     *
     */
    private final Path base;

    /**
     *
     */
    private final Options options;

    /**
     * Erzeugt eine neue Instanz von {@link AbstractGenerator}.
     *
     * @param options {@link Options}
     * @param base {@link Path}
     */
    public AbstractGenerator(final Options options, final Path base)
    {
        super();

        this.options = Objects.requireNonNull(options, "options required");
        this.base = Objects.requireNonNull(base, "base required");
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
     * @return {@link Options}
     */
    protected Options getOptions()
    {
        return this.options;
    }

    /**
     * Liefert ein Set mit allen Path-Objekten (Verzeichnisse, Dateien) das Basis-Verzeichnisses.
     *
     * @param options {@link Options}
     * @param base {@link Path}
     * @param visitOption {@link FileVisitOption}
     * @return {@link Set}
     */
    protected Set<Path> getPaths(final Options options, final Path base, final FileVisitOption[] visitOption)
    {
        final Set<Path> set;

        try (Stream<Path> stream = Files.walk(getBase(), visitOption))
        {
            set = stream.collect(Collectors.toCollection(TreeSet::new));
        }
        catch (IOException iex)
        {
            throw new UncheckedIOException(iex);
        }

        return set;
    }
}
