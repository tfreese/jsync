// Created: 29.09.2020
package de.freese.jsync.remote.pool;

import java.util.Objects;
import java.util.function.Consumer;
import de.freese.jsync.remote.api.JsyncConnection;
import de.freese.jsync.remote.api.JsyncConnectionMapper;

/**
 * @author Thomas Freese
 */
public class PoolableJsyncConnection extends JsyncConnectionMapper
{
    /**
     *
     */
    private final Consumer<PoolableJsyncConnection> pool;

    /**
     * Erstellt ein neues {@link PoolableJsyncConnection} Object.
     *
     * @param connection {@link JsyncConnection}
     * @param pool {@link Consumer}
     */
    public PoolableJsyncConnection(final JsyncConnection connection, final Consumer<PoolableJsyncConnection> pool)
    {
        super(connection);

        this.pool = Objects.requireNonNull(pool, "pool required");
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionMapper#close()
     */
    @Override
    public void close()
    {
        this.pool.accept(this);
    }

    /**
     * @return {@link JsyncConnection}
     */
    JsyncConnection getRawConnection()
    {
        return getConnection();
    }
}
