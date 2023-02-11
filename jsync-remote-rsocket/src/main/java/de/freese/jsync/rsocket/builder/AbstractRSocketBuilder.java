// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <T> Builder Type
 * @param <B> Build-Result Type
 *
 * @author Thomas Freese
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRSocketBuilder<T extends AbstractRSocketBuilder<?, ?>, B> {
    private Logger logger;

    public abstract B build();

    public T logger(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger required");

        return (T) this;
    }

    protected Logger getLogger() {
        if (this.logger == null) {
            this.logger = LoggerFactory.getLogger(getClass());
        }

        return this.logger;
    }
}
