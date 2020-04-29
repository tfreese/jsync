// Created: 05.04.2018
package de.freese.jsync.filesystem.source;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.AbstractFileSystem;

/**
 * Basis-Implementierung des {@link Source}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractSource extends AbstractFileSystem implements Source
{
    /**
     * Erzeugt eine neue Instanz von {@link AbstractSource}.
     *
     * @param options {@link Options}
     */
    public AbstractSource(final Options options)
    {
        super(options);
    }
}
