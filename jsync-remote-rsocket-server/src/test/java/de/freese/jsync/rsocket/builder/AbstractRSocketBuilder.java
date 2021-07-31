// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.rsocket.Closeable;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 *
 * @param <T> Type
 */
public abstract class AbstractRSocketBuilder<T extends AbstractRSocketBuilder<?>>
{
    /**
     *
     */
    private PayloadDecoder payloadDecoder = PayloadDecoder.ZERO_COPY;

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
     * @return {@link Mono}
     */
    public abstract Mono<? extends Closeable> build();

    /**
     * @param rSocketServer {@link RSocketServer}
     *
     * @return {@link RSocketServer}
     */
    protected RSocketServer configure(final RSocketServer rSocketServer)
    {
        RSocketServer server = rSocketServer.payloadDecoder(this.payloadDecoder);

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
        this.payloadDecoder = Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        return (T) this;
    }
}
