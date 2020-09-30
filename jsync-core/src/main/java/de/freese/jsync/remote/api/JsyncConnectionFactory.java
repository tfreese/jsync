// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface JsyncConnectionFactory
{
    /**
    *
    */
    public void close();

    /**
     * @param uri {@link URI}
     */
    public void connect(URI uri);

    /**
     * @return {@link JsyncConnection}
     */
    public JsyncConnection getConnection();
}
