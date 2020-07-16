/**
 * Created: 14.07.2020
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    private Consumer<String> currentMetaConsumer = null;

    /**
     *
     */
    private BiConsumer<Path, Integer> itemCountConsumer = null;

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
     * @see de.freese.jsync.generator.listener.GeneratorListener#currentMeta(java.lang.String)
     */
    @Override
    public void currentMeta(final String relativePath)
    {
        if (this.currentMetaConsumer != null)
        {
            this.currentMetaConsumer.accept(relativePath);
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
     * @param consumer {@link Consumer}
     */
    public void doOnCurrentMeta(final Consumer<String> consumer)
    {
        this.currentMetaConsumer = consumer;
    }

    /**
     * @param consumer {@link BiConsumer}
     */
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
