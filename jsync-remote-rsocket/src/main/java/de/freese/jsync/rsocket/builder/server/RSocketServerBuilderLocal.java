// Created: 15 Juni 2024
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
public final class RSocketServerBuilderLocal extends AbstractServerBuilder<RSocketServerBuilderLocal, Mono<Closeable>> {
    private String name;

    @Override
    public Mono<Closeable> build() {
        Objects.requireNonNull(this.name, "name required");

        final RSocketServer rSocketServer = configure(RSocketServer.create());

        final ServerTransport<Closeable> serverTransport = LocalServerTransport.create(this.name);

        return rSocketServer.bind(serverTransport);
    }

    public RSocketServerBuilderLocal name(final String name) {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }

    @Override
    protected RSocketServerBuilderLocal self() {
        return this;
    }
}
