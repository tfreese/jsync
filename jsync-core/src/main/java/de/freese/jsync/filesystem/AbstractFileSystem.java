// Created: 05.04.2018
package de.freese.jsync.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Erzeugt eine neue Instanz von {@link AbstractFileSystem}.
     */
    public AbstractFileSystem()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }
}
