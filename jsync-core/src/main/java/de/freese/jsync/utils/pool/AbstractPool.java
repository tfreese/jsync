// Created: 31.08.2020
package de.freese.jsync.utils.pool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private final List<T> pool = new ArrayList<>();

    /**
     * Erstellt ein neues {@link AbstractPool} Object.
     */
    public AbstractPool()
    {
        super();
    }

    /**
     * @param cleaner {@link Consumer}
     */
    public void destroy(final Consumer<T> cleaner)
    {
        getLock().lock();

        try
        // synchronized (this)
        {
            for (Iterator<T> iterator = this.pool.iterator(); iterator.hasNext();)
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
     * @return Object
     */
    protected abstract T createObject();

    /**
     * @param object Object
     */
    protected abstract void destroyObject(T object);

    /**
     * @return Object
     */
    public T get()
    {
        getLock().lock();

        try
        // synchronized (this)
        {
            T object = null;

            if (this.pool.isEmpty())
            {
                object = createObject();
            }
            else
            {
                object = this.pool.remove(0);
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
     * @param object Object
     */
    public void release(final T object)
    {
        getLock().lock();

        try
        // synchronized (this)
        {
            this.pool.add(object);
        }
        finally
        {
            getLock().unlock();
        }
    }
}
