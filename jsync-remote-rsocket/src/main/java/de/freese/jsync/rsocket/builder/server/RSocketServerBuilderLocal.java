// Created: 15 Juni 2024
package de.freese.jsync.rsocket.builder.server;

import java.util.Objects;
import java.util.function.UnaryOperator;

import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public final class RSocketServerBuilderLocal {
    private final RSocketServerBuilderSupport builderSupport = new RSocketServerBuilderSupport();

    private String name;

    public RSocketServerBuilderLocal addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer) {
        builderSupport.addRSocketServerCustomizer(rSocketServerCustomizer);

        return this;
    }

    public Mono<Closeable> build() {
        Objects.requireNonNull(this.name, "name required");

        final RSocketServer rSocketServer = builderSupport.configure(RSocketServer.create());

        final ServerTransport<Closeable> serverTransport = LocalServerTransport.create(this.name);

        return rSocketServer.bind(serverTransport);
    }

    public RSocketServerBuilderLocal name(final String name) {
        this.name = Objects.requireNonNull(name, "name required");

        return this;
    }

    public RSocketServerBuilderLocal payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }

    public RSocketServerBuilderLocal socketAcceptor(final SocketAcceptor socketAcceptor) {
        builderSupport.socketAcceptor(socketAcceptor);

        return this;
    }
}
