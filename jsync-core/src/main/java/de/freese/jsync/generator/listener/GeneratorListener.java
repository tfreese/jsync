/**
 * Created: 22.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.EventListener;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;

/**
 * Listener für den {@link Generator}.
 *
 * @author Thomas Freese
 */
public interface GeneratorListener extends EventListener
{
    /**
     * Setzt die Anzahl zu verarbeitender Path-Objekte (Verzeichnisse, Dateien).
     * 
     * @param path {@link Path}
     * @param pathCount int
     */
    public void pathCount(Path path, int pathCount);

    /**
     * Setzt das {@link SyncItem} (Verzeichnisse, Dateien), welches aktuell bearbeitet wird.
     *
     * @param syncItem {@link SyncItem}
     */
    public void processingSyncItem(SyncItem syncItem);
}