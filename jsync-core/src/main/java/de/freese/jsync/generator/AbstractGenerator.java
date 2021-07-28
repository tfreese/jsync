// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import de.freese.jsync.utils.io.FileVisitorHierarchie;

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
    protected Set<Path> getPaths(final Path base, final FileVisitOption[] visitOptions)
    {
        // Set<Path> set = new LinkedHashSet<>();
        Set<Path> set = new TreeSet<>();

        try
        {
            Files.walkFileTree(base, Set.of(visitOptions), Integer.MAX_VALUE, new FileVisitorHierarchie(set));
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        return set;

        // Set<Path> set = null;
        //
        // try (Stream<Path> stream = Files.walk(base, visitOptions))
        // {
        // // TODO Excludes filtern
        // // TODO Wenn Dateien fehlerhaft sind, knallt es hier bereits -> eigenen FileWalker implementieren !
        //
//            // @formatter:off
//            set = stream
//                    .collect(Collectors.toCollection(TreeSet::new))
//                    ;
//            // @formatter:on
        // }
        // catch (IOException ex)
        // {
        // throw new UncheckedIOException(ex);
        // }
        //
        // return set;
    }
}
