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
 * @author Thomas Freese
 */
public abstract class AbstractGenerator implements Generator
{
    protected Flux<Path> getPathsAsFlux(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter)
    {
        return Flux.<Path>create(sink ->
        {
            walkFileTree(base, visitOptions, pathFilter, sink::next);
            sink.complete();
        }).sort();
    }

    protected Set<Path> getPathsAsSet(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter)
    {
        Set<Path> set = new TreeSet<>();

        walkFileTree(base, visitOptions, pathFilter, set::add);

        return set;
    }

    private void walkFileTree(final Path base, final FileVisitOption[] visitOptions, final PathFilter pathFilter, final Consumer<Path> consumer)
    {
        //  Exception here if Files are corrupt, use own FileWalker !
        // try (Stream<Path> stream = Files.walk(base, visitOptions)) {...}

        try
        {
            Files.walkFileTree(base, Set.of(visitOptions), Integer.MAX_VALUE, new FileVisitorHierarchie(base, pathFilter, consumer));
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
