// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.client;

import java.util.function.UnaryOperator;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;

import de.freese.jsync.rsocket.builder.AbstractBuilder;

/**
 * @author Thomas Freese
 */
abstract class AbstractClientBuilder<T> extends AbstractBuilder<T, RSocketClient> {
    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();

    public T addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        getBuilderSupport().addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return self();
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder) {
        getBuilderSupport().payloadDecoder(payloadDecoder);

        return self();
    }

    protected RSocketClientBuilderSupport getBuilderSupport() {
        return builderSupport;
    }
}
