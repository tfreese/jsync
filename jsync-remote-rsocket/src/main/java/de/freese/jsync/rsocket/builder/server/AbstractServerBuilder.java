// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.server;

import java.util.function.UnaryOperator;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;

import de.freese.jsync.rsocket.builder.AbstractBuilder;

/**
 * @author Thomas Freese
 */
abstract class AbstractServerBuilder<T, B> extends AbstractBuilder<T, B> {
    private final RSocketServerBuilderSupport builderSupport = new RSocketServerBuilderSupport();

    public T addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer) {
        getBuilderSupport().addRSocketServerCustomizer(rSocketServerCustomizer);

        return self();
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder) {
        getBuilderSupport().payloadDecoder(payloadDecoder);

        return self();
    }

    public T socketAcceptor(final SocketAcceptor socketAcceptor) {
        getBuilderSupport().socketAcceptor(socketAcceptor);

        return self();
    }

    protected RSocketServerBuilderSupport getBuilderSupport() {
        return builderSupport;
    }
}
