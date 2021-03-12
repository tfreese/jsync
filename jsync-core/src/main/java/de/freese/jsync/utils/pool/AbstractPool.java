// Created: 31.08.2020
package de.freese.jsync.utils.pool;

import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 * @param <T> Type
 */
public abstract class AbstractPool<T>
{
    /**
    *
    */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
    *
    */
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
    *
    */
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();

    /**
     * @return Object
     */
    protected abstract T createObject();

    /**
     * @param cleaner {@link Consumer}
     */
    public void destroy(final Consumer<T> cleaner)
    {
        getLock().lock();

        try
        {
            for (Iterator<T> iterator = getPool().iterator(); iterator.hasNext();)
            {
                T object = iterator.next();

                cleaner.accept(object);

                destroyObject(object);

                iterator.remove();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @param object Object
     */
    protected abstract void destroyObject(T object);

    /**
     * @return Object
     */
    protected T get()
    {
        getLock().lock();

        try
        {
            T object = getPool().poll();

            if (object == null)
            {
                object = createObject();
            }

            return object;
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
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.LOGGER;
    }

    /**
     * @return {@link Queue}<T>
     */
    protected Queue<T> getPool()
    {
        return this.pool;
    }

    /**
     * @param object Object
     */
    public void release(final T object)
    {
        Objects.requireNonNull(object, "object required");

        getLock().lock();

        try
        {
            getPool().add(object);
        }
        finally
        {
            getLock().unlock();
        }
    }
}
