// Created: 11.07.2021
package de.freese.jsync.rsocket.server;

import java.util.Objects;
import java.util.function.Function;

import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;

/**
 * {@link RSocketServer} für Verwendung innerhalb einer Runtime.
 *
 * @author Thomas Freese
 */
public class MyRSocketServerLocal implements MyRSocketServer
{
    /**
     *
     */
    private final String name;

    /**
     *
     */
    private Closeable server;

    /**
     *
     */
    private final Function<Integer, SocketAcceptor> socketAcceptor;

    /**
     * Erstellt ein neues {@link MyRSocketServerLocal} Object.
     *
     * @param name String
     * @param socketAcceptor {@link SocketAcceptor}
     */
    public MyRSocketServerLocal(final String name, final Function<Integer, SocketAcceptor> socketAcceptor)
    {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.socketAcceptor = Objects.requireNonNull(socketAcceptor, "socketAcceptor required");
    }

    /**
     * @return String
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @see de.freese.jsync.rsocket.server.MyRSocketServer#start()
     */
    @Override
    public void start() throws Exception
    {
        ServerTransport<Closeable> serverTransport = LocalServerTransport.create(this.name);

        // @formatter:off
        this.server = RSocketServer.create()
                .acceptor(this.socketAcceptor.apply(0))
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .bindNow(serverTransport)
                ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.rsocket.server.MyRSocketServer#stop()
     */
    @Override
    public void stop()
    {
        this.server.dispose();
    }
}
