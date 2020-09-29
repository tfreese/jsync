// Created: 29.09.2020
package de.freese.jsync.remote.api;

import de.freese.jsync.model.JSyncCommand;

/**
 * @author Thomas Freese
 */
public interface JsyncRequest
{
    /**
     * @return {@link JsyncResponse}
     */
    public JsyncResponse<?> execute();

    /**
     * @param command {@link JSyncCommand}
     */
    public void setCommand(JSyncCommand command);
}
