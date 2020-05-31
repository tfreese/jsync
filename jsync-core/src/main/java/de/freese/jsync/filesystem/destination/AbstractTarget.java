// Created: 05.04.2018
package de.freese.jsync.filesystem.destination;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.AbstractFileSystem;

/**
 * Basis-Implementierung des {@link Target}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractTarget extends AbstractFileSystem implements Target
{
    /**
     * Erzeugt eine neue Instanz von {@link AbstractTarget}.
     *
     * @param options {@link Options}
     */
    public AbstractTarget(final Options options)
    {
        super(options);
    }
}
