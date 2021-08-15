// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.utils.io.FileVisitorHierarchie;
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
     * @param pathFilter {@link PathFilter}
     *
     * @return {@link Flux}
     */
    protected Flux<Path> getPathsAsFlux(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter)
    {
        return Flux.<Path> create(sink -> {
            walkFileTree(base, visitOptions, pathFilter, sink::next);
            sink.complete();
        }).sort();
    }

    /**
     * Liefert ein Set mit allen Path-Objekten (Verzeichnisse, Dateien) das Basis-Verzeichnisses.
     *
     * @param base {@link Path}
     * @param visitOptions {@link FileVisitOption}
     * @param pathFilter {@link PathFilter}
     *
     * @return {@link Set}
     */
    protected Set<Path> getPathsAsSet(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter)
    {
        Set<Path> set = new TreeSet<>();

        walkFileTree(base, visitOptions, pathFilter, set::add);

        return set;
    }

    /**
     * @param base {@link Path}
     * @param visitOptions {@link FileVisitOption}
     * @param pathFilter {@link PathFilter}
     * @param consumer {@link Consumer}
     */
    private void walkFileTree(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter, final Consumer<Path> consumer)
    {
        try
        {
            Files.walkFileTree(base, Set.of(visitOptions), Integer.MAX_VALUE, new FileVisitorHierarchie(pathFilter, consumer));
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        // try (Stream<Path> stream = Files.walk(base, visitOptions))
        // {
        // TODO Wenn Dateien fehlerhaft sind, knallt es hier bereits -> eigenen FileWalker implementieren !
        // stream.forEach(consumer);
        // }
        // catch (IOException ex)
        // {
        // throw new UncheckedIOException(ex);
        // }
    }
}
