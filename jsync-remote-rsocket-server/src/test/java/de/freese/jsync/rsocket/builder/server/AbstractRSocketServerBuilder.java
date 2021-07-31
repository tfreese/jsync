// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.freese.jsync.rsocket.builder.AbstractRSocketBuilder;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 *
 * @param <T> Builder Type
 */
public abstract class AbstractRSocketServerBuilder<T extends AbstractRSocketServerBuilder<?>> extends AbstractRSocketBuilder<T, Mono<? extends Closeable>>
{
    /**
    *
    */
    private final List<Function<RSocketServer, RSocketServer>> rSocketServerCustomizers = new ArrayList<>();

    /**
     * @param rSocketServerCustomizer {@link Function}
     *
     * @return {@link AbstractRSocketBuilder}
     */
    @SuppressWarnings("unchecked")
    public T addRSocketServerCustomizer(final Function<RSocketServer, RSocketServer> rSocketServerCustomizer)
    {
        this.rSocketServerCustomizers.add(Objects.requireNonNull(rSocketServerCustomizer, "rSocketServerCustomizer required"));

        return (T) this;
    }

    /**
     * @param rSocketServer {@link RSocketServer}
     *
     * @return {@link RSocketServer}
     */
    protected RSocketServer configure(final RSocketServer rSocketServer)
    {
        RSocketServer server = rSocketServer;

        for (Function<RSocketServer, RSocketServer> serverCustomizer : this.rSocketServerCustomizers)
        {
            server = serverCustomizer.apply(server);
        }

        return server;
    }

    /**
     * @param payloadDecoder {@link PayloadDecoder}
     *
     * @return {@link AbstractRSocketBuilder}
     */
    @SuppressWarnings("unchecked")
    public T payloadDecoder(final PayloadDecoder payloadDecoder)
    {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.payloadDecoder(payloadDecoder));

        return (T) this;
    }

    /**
     * @param socketAcceptor {@link SocketAcceptor}
     *
     * @return {@link AbstractRSocketServerBuilder}
     */
    @SuppressWarnings("unchecked")
    public T socketAcceptor(final SocketAcceptor socketAcceptor)
    {
        Objects.requireNonNull(socketAcceptor, "socketAcceptor required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.acceptor(socketAcceptor));

        return (T) this;
    }
}
