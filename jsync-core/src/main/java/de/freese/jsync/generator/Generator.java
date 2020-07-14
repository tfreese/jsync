/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.generator;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.NavigableMap;
import de.freese.jsync.Options;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;

/**
 * Der Generator sammelt alle relevanten Informationens des Dateisystems für den gewählten {@link Path}.
 *
 * @author Thomas Freese
 * @see SyncItem
 */
public interface Generator
{
    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    public static final FileVisitOption[] FILEVISITOPTION_NO_SYNLINKS = new FileVisitOption[0];

    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    public static final FileVisitOption[] FILEVISITOPTION_WITH_SYMLINKS = new FileVisitOption[]
    {
            FileVisitOption.FOLLOW_LINKS
    };

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    public static final LinkOption[] LINKOPTION_NO_SYMLINKS = new LinkOption[]
    {
            LinkOption.NOFOLLOW_LINKS
    };

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    public static final LinkOption[] LINKOPTION_WITH_SYMLINKS = new LinkOption[0];

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param options {@link Options}
     * @param base {@link Path}; Basis-Verzeichnis
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link NavigableMap}
     */
    public NavigableMap<String, SyncItem> createSyncItems(Options options, final Path base, GeneratorListener listener);
}
