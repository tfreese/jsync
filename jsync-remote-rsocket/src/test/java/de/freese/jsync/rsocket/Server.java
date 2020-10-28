// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

/**
 * @author Thomas Freese
 */
class Server implements Disposable
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     *
     */
    private CloseableChannel channel;

    /**
     * Erzeugt ein neues {@link Server} Objekt.
     */
    Server()
    {
        super();
    }

    /**
     * @see reactor.core.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        this.channel.dispose();
    }

    /**
     * @see reactor.core.Disposable#isDisposed()
     */
    @Override
    public boolean isDisposed()
    {
        return this.channel.isDisposed();
    }

    /**
     * @param port int
     * @throws Exception Falls was schief geht
     */
    public void start(final int port) throws Exception
    {
        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(port)
                .runOn(LoopResources.create("server-" + port, 1, 2, true))
                ;
        // @formatter:on

        SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload -> {
            LOGGER.info("Server {} got {}", port, payload.getDataUtf8());
            return Mono.just(DefaultPayload.create("Server " + port + " response")).delayElement(Duration.ofMillis(100));
        });

        // @formatter:off
        this.channel = RSocketServer
            .create()
            .acceptor(socketAcceptor)
            .bindNow(TcpServerTransport.create(tcpServer))
            ;
        // @formatter:on
    }

    /**
     *
     */
    public void stop()
    {
        dispose();
    }
}