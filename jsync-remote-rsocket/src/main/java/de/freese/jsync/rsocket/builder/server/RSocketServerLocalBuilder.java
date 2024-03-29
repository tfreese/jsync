// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.server;

import java.util.Objects;

import io.rsocket.Closeable;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class RSocketServerLocalBuilder extends AbstractRSocketServerBuilder<RSocketServerLocalBuilder> {
    private String name;

    @Override
    public Mono<Closeable> build() {
        final RSocketServer rSocketServer = configure(RSocketServer.create());

        final ServerTransport<Closeable> serverTransport = LocalServerTransport.create(Objects.requireNonNull(this.name, "name required"));

        return rSocketServer.bind(serverTransport);
    }

    public RSocketServerLocalBuilder name(final String name) {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }
}
