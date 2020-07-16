/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.List;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncItemMeta;

/**
 * Der Generator sammelt alle relevanten Informationens des Dateisystems für den gewählten {@link Path}.
 *
 * @author Thomas Freese
 * @see SyncItem
 */
public interface Generator
{
    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param basePath String
     * @param followSymLinks boolean
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link List}
     */
    public List<SyncItem> generateItems(final String basePath, boolean followSymLinks, GeneratorListener listener);

    /**
     * Erzeugt die Meta-Daten des SyncItems (Verzeichnisse, Dateien).<br>
     *
     * @param basePath String
     * @param relativePath String
     * @param followSymLinks boolean
     * @param withChecksum boolean
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link List}
     */
    public SyncItemMeta generateMeta(final String basePath, String relativePath, boolean followSymLinks, boolean withChecksum, GeneratorListener listener);
}
