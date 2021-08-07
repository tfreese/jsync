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
public class RSocketClientLocalBuilder extends AbstractRSocketClientBuilder<RSocketClientLocalBuilder>
{
    /**
     *
     */
    private String name;

    /**
     * @see de.freese.jsync.rsocket.builder.AbstractRSocketBuilder#build()
     */
    @Override
    public RSocketClient build()
    {
        Objects.requireNonNull(this.name, "name required");

        RSocketConnector rSocketConnector = configure(RSocketConnector.create());

        ClientTransport clientTransport = LocalClientTransport.create(this.name);

        Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    /**
     * @param name String
     *
     * @return {@link RSocketClientLocalBuilder}
     */
    public RSocketClientLocalBuilder name(final String name)
    {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }
}
