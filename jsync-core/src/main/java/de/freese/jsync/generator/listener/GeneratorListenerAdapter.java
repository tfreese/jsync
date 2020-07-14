/**
 * Created: 14.07.2020
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class GeneratorListenerAdapter implements GeneratorListener
{
    /**
     *
     */
    private BiConsumer<Long, Long> checksumConsumer = null;

    /**
     *
     */
    private BiConsumer<Path, Integer> pathCountConsumer = null;

    /**
     *
     */
    private Consumer<SyncItem> syncItemConsumer = null;

    /**
     * Erstellt ein neues {@link GeneratorListenerAdapter} Object.
     */
    public GeneratorListenerAdapter()
    {
        super();
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
     */
    @Override
    public final void checksum(final long size, final long bytesRead)
    {
        if (this.checksumConsumer != null)
        {
            this.checksumConsumer.accept(size, bytesRead);
        }
    }

    /**
     * @param consumer {@link BiConsumer}
     */
    public void doOnChecksum(final BiConsumer<Long, Long> consumer)
    {
        this.checksumConsumer = consumer;
    }

    /**
     * @param consumer {@link BiConsumer}
     */
    public void doOnPathCount(final BiConsumer<Path, Integer> consumer)
    {
        this.pathCountConsumer = consumer;
    }

    /**
     * @param consumer {@link Consumer}
     */
    public void doOnSyncItem(final Consumer<SyncItem> consumer)
    {
        this.syncItemConsumer = consumer;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
     */
    @Override
    public final void pathCount(final Path path, final int pathCount)
    {
        if (this.pathCountConsumer != null)
        {
            this.pathCountConsumer.accept(path, pathCount);
        }
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#syncItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public final void syncItem(final SyncItem syncItem)
    {
        if (this.syncItemConsumer != null)
        {
            this.syncItemConsumer.accept(syncItem);
        }
    }
}
