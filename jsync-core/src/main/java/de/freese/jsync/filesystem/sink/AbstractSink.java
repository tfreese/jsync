// Created: 05.04.2018
package de.freese.jsync.filesystem.sink;

import de.freese.jsync.Options;
import de.freese.jsync.filesystem.AbstractFileSystem;

/**
 * Basis-Implementierung des {@link Sink}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractSink extends AbstractFileSystem implements Sink
{
    /**
     * Erzeugt eine neue Instanz von {@link AbstractSink}.
     *
     * @param options {@link Options}
     */
    public AbstractSink(final Options options)
    {
        super(options);
    }
}
