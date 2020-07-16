/**
 * Created: 22.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.EventListener;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItemMeta;

/**
 * Listener für den {@link Generator}.
 *
 * @author Thomas Freese
 */
public interface GeneratorListener extends EventListener
{
    /**
     * Progress der Prüfsummenbildung.
     *
     * @param size long
     * @param bytesRead long
     */
    public void checksum(long size, long bytesRead);

    /**
     * {@link SyncItemMeta} (Verzeichnisse, Dateien), welches aktuell bearbeitet wird.
     *
     * @param relativePath {@link String}
     */
    public void currentMeta(String relativePath);

    /**
     * Anzahl zu verarbeitender Path-Objekte (Verzeichnisse, Dateien).
     *
     * @param path {@link Path}
     * @param itemCount int
     */
    public void itemCount(Path path, int itemCount);
}