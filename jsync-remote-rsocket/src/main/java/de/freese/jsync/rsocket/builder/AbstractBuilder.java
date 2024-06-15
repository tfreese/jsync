// Created: 15.06.2024
package de.freese.jsync.rsocket.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractBuilder<T, B> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract B build();

    protected Logger getLogger() {
        return logger;
    }

    protected abstract T self();
}
