// Created: 10.09.2020
package de.freese.jsync.utils;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Thomas Freese
 */
public class JSyncThreadFactory implements ThreadFactory {
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public JSyncThreadFactory(final String namePrefix) {
        super();

        this.namePrefix = Objects.requireNonNull(namePrefix, "namePrefix required");
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = defaultThreadFactory.newThread(r);

        thread.setName(namePrefix + threadNumber.getAndIncrement());
        thread.setDaemon(true);

        return thread;
    }
}
