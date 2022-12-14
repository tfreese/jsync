// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import de.freese.jsync.rsocket.builder.AbstractRSocketBuilder;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Mono;

/**
 * @param <T> Builder Type
 *
 * @author Thomas Freese
 */
public abstract class AbstractRSocketServerBuilder<T extends AbstractRSocketServerBuilder<?>> extends AbstractRSocketBuilder<T, Mono<? extends Closeable>>
{
    private final List<UnaryOperator<RSocketServer>> rSocketServerCustomizers = new ArrayList<>();

    public T addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer)
    {
        this.rSocketServerCustomizers.add(Objects.requireNonNull(rSocketServerCustomizer, "rSocketServerCustomizer required"));

        return (T) this;
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder)
    {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.payloadDecoder(payloadDecoder));

        return (T) this;
    }

    public T socketAcceptor(final SocketAcceptor socketAcceptor)
    {
        Objects.requireNonNull(socketAcceptor, "socketAcceptor required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.acceptor(socketAcceptor));

        return (T) this;
    }

    protected RSocketServer configure(final RSocketServer rSocketServer)
    {
        RSocketServer server = rSocketServer;

        for (UnaryOperator<RSocketServer> serverCustomizer : this.rSocketServerCustomizers)
        {
            server = serverCustomizer.apply(server);
        }

        return server;
    }
}
