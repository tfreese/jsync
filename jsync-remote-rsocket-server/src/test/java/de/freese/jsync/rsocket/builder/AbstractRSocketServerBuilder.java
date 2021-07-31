// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

import java.util.Objects;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;

/**
 * @author Thomas Freese
 *
 * @param <T> Type
 */
public abstract class AbstractRSocketServerBuilder<T extends AbstractRSocketServerBuilder<?>> extends AbstractRSocketBuilder<AbstractRSocketServerBuilder<?>>
{
    /**
    *
    */
    private SocketAcceptor socketAcceptor;

    /**
     * @see de.freese.jsync.rsocket.builder.AbstractRSocketBuilder#configure(io.rsocket.core.RSocketServer)
     */
    @Override
    protected RSocketServer configure(final RSocketServer rSocketServer)
    {
        RSocketServer server = super.configure(rSocketServer);

        return server.acceptor(Objects.requireNonNull(this.socketAcceptor, "socketAcceptor required"));
    }

    /**
     * @param socketAcceptor {@link SocketAcceptor}
     *
     * @return {@link AbstractRSocketServerBuilder}
     */
    @SuppressWarnings("unchecked")
    public T socketAcceptor(final SocketAcceptor socketAcceptor)
    {
        this.socketAcceptor = Objects.requireNonNull(socketAcceptor, "socketAcceptor required");

        return (T) this;
    }
}
