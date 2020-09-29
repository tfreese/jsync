// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.io.Closeable;
import java.net.URI;

/**
 * @author Thomas Freese
 */
public interface JsyncConnectionFactory extends Closeable
{
    /**
     * @param uri {@link URI}
     */
    public void connect(URI uri);

    /**
     * @return {@link JsyncConnection}
     */
    public JsyncConnection getConnection();
}
