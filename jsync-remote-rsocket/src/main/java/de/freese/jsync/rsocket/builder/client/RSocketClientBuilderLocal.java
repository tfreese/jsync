// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.util.Objects;
import java.util.function.UnaryOperator;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.local.LocalClientTransport;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class RSocketClientBuilderLocal {
    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();

    private String name;

    public RSocketClientBuilderLocal addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        builderSupport.addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return this;
    }

    public RSocketClient build() {
        Objects.requireNonNull(this.name, "name required");

        final RSocketConnector rSocketConnector = builderSupport.configure(RSocketConnector.create());

        final ClientTransport clientTransport = LocalClientTransport.create(this.name);

        final Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    public RSocketClientBuilderLocal name(final String name) {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }

    public RSocketClientBuilderLocal payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }
}
