// Created: 15.06.2024
package de.freese.jsync.rsocket.builder;

import java.util.Objects;

import org.slf4j.Logger;

/**
 * @author Thomas Freese
 */
public abstract class AbstractBuilder<T extends AbstractBuilder<?, B>, B> {
    private Logger logger;

    public abstract B build();

    public T logger(final Logger logger) {
        Objects.requireNonNull(logger, "logger required");

        this.logger = logger;

        return self();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected abstract T self();
}
