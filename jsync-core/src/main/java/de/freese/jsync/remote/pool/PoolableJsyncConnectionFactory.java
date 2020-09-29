// Created: 29.09.2020
package de.freese.jsync.remote.pool;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import de.freese.jsync.remote.api.JsyncConnectionFactory;
import de.freese.jsync.remote.api.JsyncConnectionFactoryMapper;

/**
 * @author Thomas Freese
 */
public class PoolableJsyncConnectionFactory extends JsyncConnectionFactoryMapper
{
    /**
    *
    */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
    *
    */
    private final Queue<PoolableJsyncConnection> pool = new ConcurrentLinkedQueue<>();

    /**
     * Erstellt ein neues {@link PoolableJsyncConnectionFactory} Object.
     *
     * @param connectionFactory {@link JsyncConnectionFactory}
     */
    public PoolableJsyncConnectionFactory(final JsyncConnectionFactory connectionFactory)
    {
        super(connectionFactory);
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactoryMapper#close()
     */
    @Override
    public void close()
    {
        getLock().lock();

        try
        {
            for (Iterator<PoolableJsyncConnection> iterator = getPool().iterator(); iterator.hasNext();)
            {
                PoolableJsyncConnection connection = iterator.next();

                connection.getRawConnection().close();

                iterator.remove();
            }
        }
        finally
        {
            getLock().unlock();
        }

        super.close();
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactoryMapper#getConnection()
     */
    @Override
    public PoolableJsyncConnection getConnection()
    {
        getLock().lock();

        try
        {
            PoolableJsyncConnection connection = getPool().poll();

            if (connection == null)
            {
                connection = new PoolableJsyncConnection(super.getConnection(), getPool()::add);
            }

            return connection;
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return {@link ReentrantLock}
     */
    protected ReentrantLock getLock()
    {
        return this.lock;
    }

    /**
     * @return {@link Queue}<PoolableJsyncConnection>
     */
    protected Queue<PoolableJsyncConnection> getPool()
    {
        return this.pool;
    }
}
