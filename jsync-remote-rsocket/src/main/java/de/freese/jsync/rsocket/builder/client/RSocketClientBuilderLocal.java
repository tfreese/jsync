// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.util.Objects;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.local.LocalClientTransport;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class RSocketClientBuilderLocal extends AbstractClientBuilder<RSocketClientBuilderLocal> {

    private String name;

    @Override
    public RSocketClient build() {
        Objects.requireNonNull(this.name, "name required");

        final RSocketConnector rSocketConnector = getBuilderSupport().configure(RSocketConnector.create());

        final ClientTransport clientTransport = LocalClientTransport.create(this.name);

        final Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    public RSocketClientBuilderLocal name(final String name) {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }

    @Override
    protected RSocketClientBuilderLocal self() {
        return this;
    }
}
