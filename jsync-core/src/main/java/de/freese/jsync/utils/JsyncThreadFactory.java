// Created: 10.09.2020
package de.freese.jsync.utils;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Thomas Freese
 */
public class JsyncThreadFactory implements ThreadFactory
{
    /**
    *
    */
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    /**
     *
     */
    private final String namePrefix;

    /**
     *
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    /**
     * Erstellt ein neues {@link JsyncThreadFactory} Object.
     *
     * @param namePrefix String
     */
    public JsyncThreadFactory(final String namePrefix)
    {
        super();

        this.namePrefix = Objects.requireNonNull(namePrefix, "namePrefix required");
    }

    /**
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    @Override
    public Thread newThread(final Runnable r)
    {
        Thread thread = this.defaultThreadFactory.newThread(r);

        thread.setName(this.namePrefix + this.threadNumber.getAndIncrement());
        thread.setDaemon(true);

        return thread;
    }
}
