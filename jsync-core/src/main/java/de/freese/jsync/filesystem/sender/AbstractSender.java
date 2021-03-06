// Created: 05.04.2018
package de.freese.jsync.filesystem.sender;

import de.freese.jsync.filesystem.AbstractFileSystem;

/**
 * Basis-Implementierung des {@link Sender}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractSender extends AbstractFileSystem implements Sender
{
    /**
     * Erzeugt eine neue Instanz von {@link AbstractSender}.
     */
    protected AbstractSender()
    {
        super();
    }
}
