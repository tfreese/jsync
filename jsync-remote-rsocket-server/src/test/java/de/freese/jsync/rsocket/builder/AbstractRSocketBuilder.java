// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 *
 * @param <T> Builder Type
 * @param <B> Build-Result Type
 */
public abstract class AbstractRSocketBuilder<T extends AbstractRSocketBuilder<?, ?>, B>
{
    /**
     *
     */
    private Logger logger;

    /**
     * @return Object
     */
    public abstract B build();

    /**
     * @return Logger
     */
    protected Logger getLogger()
    {
        if (this.logger == null)
        {
            this.logger = LoggerFactory.getLogger(getClass());
        }

        return this.logger;
    }

    /**
     * @param logger {@link Logger}
     *
     * @return {@link AbstractRSocketBuilder}
     */
    @SuppressWarnings("unchecked")
    public T logger(final Logger logger)
    {
        this.logger = Objects.requireNonNull(logger, "logger required");

        return (T) this;
    }
}
