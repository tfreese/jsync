// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import de.freese.jsync.rsocket.builder.AbstractRSocketBuilder;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;

/**
 * @author Thomas Freese
 */
@SuppressWarnings("unchecked")
public abstract class AbstractRSocketClientBuilder<T extends AbstractRSocketClientBuilder<?>> extends AbstractRSocketBuilder<T, RSocketClient>
{
    private final List<UnaryOperator<RSocketConnector>> rSocketConnectorCustomizers = new ArrayList<>();

    public T addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer)
    {
        this.rSocketConnectorCustomizers.add(Objects.requireNonNull(rSocketConnectorCustomizer, "rSocketConnectorCustomizer required"));

        return (T) this;
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder)
    {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.payloadDecoder(payloadDecoder));

        return (T) this;
    }

    protected RSocketConnector configure(final RSocketConnector rSocketConnector)
    {
        RSocketConnector connector = rSocketConnector;

        for (UnaryOperator<RSocketConnector> connectorCustomizer : this.rSocketConnectorCustomizers)
        {
            connector = connectorCustomizer.apply(connector);
        }

        return connector;
    }
}
