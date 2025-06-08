// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;

import de.freese.jsync.rsocket.builder.AbstractBuilder;

/**
 * @author Thomas Freese
 */
abstract class AbstractServerBuilder<T extends AbstractBuilder<?, B>, B> extends AbstractBuilder<T, B> {
    private final List<UnaryOperator<RSocketServer>> rSocketServerCustomizers = new ArrayList<>();

    public T addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer) {
        rSocketServerCustomizers.add(Objects.requireNonNull(rSocketServerCustomizer, "rSocketServerCustomizer required"));

        return self();
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder) {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        return addRSocketServerCustomizer(rSocketServer -> rSocketServer.payloadDecoder(payloadDecoder));
    }

    public T socketAcceptor(final SocketAcceptor socketAcceptor) {
        Objects.requireNonNull(socketAcceptor, "socketAcceptor required");

        return addRSocketServerCustomizer(rSocketServer -> rSocketServer.acceptor(socketAcceptor));
    }

    protected RSocketServer configure(final RSocketServer rSocketServer) {
        RSocketServer server = rSocketServer;

        for (UnaryOperator<RSocketServer> serverCustomizer : rSocketServerCustomizers) {
            server = serverCustomizer.apply(server);
        }

        return server;
    }
}
