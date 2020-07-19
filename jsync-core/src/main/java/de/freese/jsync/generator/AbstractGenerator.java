// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractGenerator implements Generator
{
    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    protected static final FileVisitOption[] FILEVISITOPTION_NO_SYNLINKS = new FileVisitOption[0];

    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    protected static final FileVisitOption[] FILEVISITOPTION_WITH_SYMLINKS = new FileVisitOption[]
    {
            FileVisitOption.FOLLOW_LINKS
    };

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    protected static final LinkOption[] LINKOPTION_NO_SYMLINKS = new LinkOption[]
    {
            LinkOption.NOFOLLOW_LINKS
    };

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    protected static final LinkOption[] LINKOPTION_WITH_SYMLINKS = new LinkOption[0];

    /**
    *
    */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erzeugt eine neue Instanz von {@link AbstractGenerator}.
     */
    public AbstractGenerator()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * Liefert ein Set mit allen Path-Objekten (Verzeichnisse, Dateien) das Basis-Verzeichnisses.
     *
     * @param base {@link Path}
     * @param visitOptions {@link FileVisitOption}
     * @return {@link Set}
     */
    protected Set<Path> getPaths(final Path base, final FileVisitOption[] visitOptions)
    {
        Set<Path> set = null;

        try (Stream<Path> stream = Files.walk(base, visitOptions))
        {
            // @formatter:off
            set = stream
                    //.filter(predicate) // TODO Excludes filtern
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
