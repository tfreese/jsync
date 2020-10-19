// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
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
        server.start(8888);
        server.stop();
    }

    /**
     *
     */
    private Disposable server;

    /**
     * Erstellt ein neues {@link JsyncRSocketServer} Object.
     */
    public JsyncRSocketServer()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param port int
     */
    public void start(final int port)
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(
                        Retry
                            .fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                            .doBeforeRetry(s -> LOGGER.debug("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // TcpResources.set(LoopResources.create("jsync-server", 2, 4, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(port)
                .runOn(LoopResources.create("jsync-server", 2, 4, true))
                ;
        // @formatter:on

        // @formatter:off
         this.server = RSocketServer
                .create(SocketAcceptor.with(new JsyncRSocketHandler()))
                .resume(resume)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                //.bind(TcpServerTransport.create("localhost", 7000))
                .bind(TcpServerTransport.create(tcpServer))
                //.subscribe()
                .block()
                ;
        // @formatter:on
    }

    /**
    *
    */
    public void stop()
    {
        this.server.dispose();
    }
}
