// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import de.freese.jsync.filesystem.AbstractFileSystem;

/**
 * Basis-Implementierung des {@link Receiver}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractReceiver extends AbstractFileSystem implements Receiver
{
    /**
     * Erzeugt eine neue Instanz von {@link AbstractReceiver}.
     */
    protected AbstractReceiver()
    {
        super();
    }
}
