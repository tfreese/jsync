// Created: 14.07.2020
package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class GeneratorListenerAdapter implements GeneratorListener
{
    private LongConsumer checksumConsumer;

    private Consumer<SyncItem> currentItemConsumer;

    private BiConsumer<Path, Integer> itemCountConsumer;

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long)
     */
    @Override
    public final void checksum(final long bytesRead)
    {
        if (this.checksumConsumer != null)
        {
            this.checksumConsumer.accept(bytesRead);
        }
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#currentItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void currentItem(final SyncItem syncItem)
    {
        if (this.currentItemConsumer != null)
        {
            this.currentItemConsumer.accept(syncItem);
        }
    }

    public void doOnChecksum(final LongConsumer consumer)
    {
        this.checksumConsumer = consumer;
    }

    public void doOnCurrentItem(final Consumer<SyncItem> consumer)
    {
        this.currentItemConsumer = consumer;
    }

    public void doOnItemCount(final BiConsumer<Path, Integer> consumer)
    {
        this.itemCountConsumer = consumer;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#itemCount(java.nio.file.Path, int)
     */
    @Override
    public void itemCount(final Path path, final int itemCount)
    {
        if (this.itemCountConsumer != null)
        {
            this.itemCountConsumer.accept(path, itemCount);
        }
    }
}
