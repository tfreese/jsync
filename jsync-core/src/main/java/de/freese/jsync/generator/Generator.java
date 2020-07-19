/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.List;
import java.util.function.LongConsumer;
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
     * Erzeugt die Prüfsumme einer Datei.<br>
     *
     * @param basePath String
     * @param relativePath String
     * @param consumerBytesRead {@link LongConsumer}; optional
     * @return String
     */
    public String generateChecksum(final String basePath, String relativePath, final LongConsumer consumerBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses alphabetisch sortiert.<br>
     *
     * @param basePath String
     * @param followSymLinks boolean
     * @return {@link List}
     */
    public List<SyncItem> generateItems(final String basePath, boolean followSymLinks);
}
