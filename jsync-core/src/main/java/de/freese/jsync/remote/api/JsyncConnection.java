// Created: 29.09.2020
package de.freese.jsync.remote.api;

/**
 * @author Thomas Freese
 */
public interface JsyncConnection
{
    /**
     *
     */
    public void close();

    /**
     * @return {@link JsyncRequest}
     */
    public JsyncRequest createRequest();
}
