// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.net.URI;
import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class JsyncConnectionFactoryMapper implements JsyncConnectionFactory
{
    /**
     *
     */
    private final JsyncConnectionFactory connectionFactory;

    /**
     * Erstellt ein neues {@link JsyncConnectionFactoryMapper} Object.
     *
     * @param connectionFactory {@link JsyncConnectionFactory}
     */
    public JsyncConnectionFactoryMapper(final JsyncConnectionFactory connectionFactory)
    {
        super();

        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory required");
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#close()
     */
    @Override
    public void close()
    {
        getConnectionFactory().close();
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        getConnectionFactory().connect(uri);
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#getConnection()
     */
    @Override
    public JsyncConnection getConnection()
    {
        return getConnectionFactory().getConnection();
    }

    /**
     * @return {@link JsyncConnectionFactory}
     */
    protected JsyncConnectionFactory getConnectionFactory()
    {
        return this.connectionFactory;
    }

}