// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Hooks;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class JsyncRSocketServer
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketServer.class);

    /**
     * @param args String[]
     */
    public static void main(final String[] args)
    {
        JsyncRSocketServer server = new JsyncRSocketServer();
        server.start(8888, 1, 4);
        // server.stop();
    }

    /**
     *
     */
    private Disposable server;

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param port int
     * @param selectCount int
     * @param workerCount int
     */
    public void start(final int port, final int selectCount, final int workerCount)
    {
        getLogger().info("starting jsync-rsocket server on port: {}", port);

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        Hooks.onErrorDropped(th -> LOGGER.error(th.getMessage()));

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(
                        Retry
                            .fixedDelay(10, Duration.ofSeconds(1))
                            .doBeforeRetry(s -> LOGGER.debug("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("jsync-server", 2, 4, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(port)
                .runOn(LoopResources.create("jsync-server-", selectCount, workerCount, false))
                ;
        // @formatter:on

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<Closeable> serverTransport = LocalServerTransport.create("test-local-" + port);

        SocketAcceptor socketAcceptor = SocketAcceptor.with(new JsyncRSocketHandler());

        // @formatter:off
         this.server = RSocketServer.create()
                .acceptor(socketAcceptor)
                .resume(resume)
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .bindNow(serverTransport)
                ;
        // @formatter:on
    }

    /**
    *
    */
    public void stop()
    {
        getLogger().info("stopping jsync-rsocket server");

        this.server.dispose();
    }
}
