// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class JsyncConnectionMapper implements JsyncConnection
{
    /**
     *
     */
    private final JsyncConnection connection;

    /**
     * Erstellt ein neues {@link JsyncConnectionMapper} Object.
     *
     * @param connection {@link JsyncConnection}
     */
    public JsyncConnectionMapper(final JsyncConnection connection)
    {
        super();

        this.connection = Objects.requireNonNull(connection, "connection required");
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnection#close()
     */
    @Override
    public void close()
    {
        getConnection().close();
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnection#createRequest()
     */
    @Override
    public JsyncRequest createRequest()
    {
        return getConnection().createRequest();
    }

    /**
     * @return {@link JsyncConnection}
     */
    public JsyncConnection getConnection()
    {
        return this.connection;
    }

}
