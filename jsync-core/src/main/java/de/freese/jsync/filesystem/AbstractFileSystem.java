// Created: 05.04.2018
package de.freese.jsync.filesystem;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractFileSystem implements FileSystem
{
    /**
    *
    */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
    *
    */
    private final Options options;

    /**
     * Erzeugt eine neue Instanz von {@link AbstractFileSystem}.
     *
     * @param options {@link Options}
     */
    public AbstractFileSystem(final Options options)
    {
        super();

        this.options = Objects.requireNonNull(options, "options required");
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link Options}
     */
    protected Options getOptions()
    {
        return this.options;
    }
}
