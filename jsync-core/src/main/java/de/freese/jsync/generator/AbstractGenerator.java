// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;

/**
 * Basis-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractGenerator implements Generator
{
    /**
     * Liefert ein Set mit allen Path-Objekten (Verzeichnisse, Dateien) das Basis-Verzeichnisses.
     *
     * @param base {@link Path}
     * @param visitOptions {@link FileVisitOption}
     *
     * @return {@link Set}
     */
    protected Flux<Path> getPathsAsFlux(final Path base, final FileVisitOption[] visitOptions)
    {
        try
        {
            // TODO Excludes filtern
            return Flux.fromStream(Files.walk(base, visitOptions));
        }
        catch (IOException iex)
        {
            throw new UncheckedIOException(iex);
        }
    }

    /**
     * Liefert ein Set mit allen Path-Objekten (Verzeichnisse, Dateien) das Basis-Verzeichnisses.
     *
     * @param base {@link Path}
     * @param visitOptions {@link FileVisitOption}
     *
     * @return {@link Set}
     */
    protected Set<Path> getPathsAsStream(final Path base, final FileVisitOption[] visitOptions)
    {
        Set<Path> set = null;

        try (Stream<Path> stream = Files.walk(base, visitOptions))
        {
            // TODO Excludes filtern
            // @formatter:off
            set = stream
                    .collect(Collectors.toCollection(TreeSet::new))
                    ;
            // @formatter:on
        }
        catch (IOException iex)
        {
            throw new UncheckedIOException(iex);
        }

        return set;
    }
}
