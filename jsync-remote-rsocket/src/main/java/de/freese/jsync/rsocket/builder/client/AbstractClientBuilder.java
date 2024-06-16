// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;

import de.freese.jsync.rsocket.builder.AbstractBuilder;

/**
 * @author Thomas Freese
 */
abstract class AbstractClientBuilder<T extends AbstractBuilder<?, RSocketClient>> extends AbstractBuilder<T, RSocketClient> {
    private final List<UnaryOperator<RSocketConnector>> rSocketConnectorCustomizers = new ArrayList<>();

    public T addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        rSocketConnectorCustomizers.add(Objects.requireNonNull(rSocketConnectorCustomizer, "rSocketConnectorCustomizer required"));

        return self();
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder) {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        return addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.payloadDecoder(payloadDecoder));
    }

    protected RSocketConnector configure(final RSocketConnector rSocketConnector) {
        RSocketConnector connector = rSocketConnector;

        for (UnaryOperator<RSocketConnector> connectorCustomizer : this.rSocketConnectorCustomizers) {
            connector = connectorCustomizer.apply(connector);
        }

        return connector;
    }
}
