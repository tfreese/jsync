/**
 * Created: 12.07.2020
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import de.freese.jsync.model.SyncItem;

/**
 * Leere Implementierung.
 *
 * @author Thomas Freese
 */
public class NoOpGeneratorListener implements GeneratorListener
{
    /**
     *
     */
    public static final GeneratorListener INSTANCE = new NoOpGeneratorListener();

    /**
     * Erstellt ein neues {@link NoOpGeneratorListener} Object.
     */
    private NoOpGeneratorListener()
    {
        super();
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
     */
    @Override
    public void checksum(final long size, final long bytesRead)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
     */
    @Override
    public void pathCount(final Path path, final int pathCount)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void processingSyncItem(final SyncItem syncItem)
    {
        // Empty
    }
}
