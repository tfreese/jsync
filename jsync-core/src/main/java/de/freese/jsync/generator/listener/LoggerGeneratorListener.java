/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.client.listener.ClientListener;

/**
 * {@link Logger}-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
public class LoggerGeneratorListener extends AbstractGeneratorListener
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger("Generator");

    /**
     *
     */
    private final String prefix;

    /**
     * Erstellt ein neues {@link LoggerGeneratorListener} Object.
     *
     * @param prefix String
     */
    public LoggerGeneratorListener(final String prefix)
    {
        super();

        this.prefix = Objects.requireNonNull(prefix, "prefix required");
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
     * @see de.freese.jsync.generator.listener.GeneratorListener#currentMeta(java.lang.String)
     */
    @Override
    public void currentMeta(final String relativePath)
    {
        String message = currentMetaMessage(relativePath, this.prefix);

        getLogger().debug(message);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#itemCount(java.nio.file.Path, int)
     */
    @Override
    public void itemCount(final Path path, final int itemCount)
    {
        String message = itemCountMessage(path, itemCount, this.prefix);

        getLogger().info(message);
    }
}
