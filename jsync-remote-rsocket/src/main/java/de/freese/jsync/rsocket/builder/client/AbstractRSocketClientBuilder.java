// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.freese.jsync.rsocket.builder.AbstractRSocketBuilder;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;

/**
 * @author Thomas Freese
 *
 * @param <T> Builder Type
 */
public abstract class AbstractRSocketClientBuilder<T extends AbstractRSocketClientBuilder<?>> extends AbstractRSocketBuilder<T, RSocketClient>
{
    /**
    *
    */
    private final List<Function<RSocketConnector, RSocketConnector>> rSocketConnectorCustomizers = new ArrayList<>();

    /**
     * @param rSocketConnectorCustomizer {@link Function}
     *
     * @return {@link AbstractRSocketBuilder}
     */
    @SuppressWarnings("unchecked")
    public T addRSocketConnectorCustomizer(final Function<RSocketConnector, RSocketConnector> rSocketConnectorCustomizer)
    {
        this.rSocketConnectorCustomizers.add(Objects.requireNonNull(rSocketConnectorCustomizer, "rSocketConnectorCustomizer required"));

        return (T) this;
    }

    /**
     * @param rSocketConnector {@link RSocketConnector}
     *
     * @return {@link RSocketConnector}
     */
    protected RSocketConnector configure(final RSocketConnector rSocketConnector)
    {
        RSocketConnector connector = rSocketConnector;

        for (Function<RSocketConnector, RSocketConnector> connectorCustomizer : this.rSocketConnectorCustomizers)
        {
            connector = connectorCustomizer.apply(connector);
        }

        return connector;
    }

    /**
     * @param payloadDecoder {@link PayloadDecoder}
     *
     * @return {@link AbstractRSocketBuilder}
     */
    @SuppressWarnings("unchecked")
    public T payloadDecoder(final PayloadDecoder payloadDecoder)
    {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.payloadDecoder(payloadDecoder));

        return (T) this;
    }
}
