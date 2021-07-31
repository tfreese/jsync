// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
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
import reactor.netty.tcp.TcpResources;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public final class JsyncRSocketServer
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketServer.class);

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        JsyncRSocketServer server = new JsyncRSocketServer();

        server.start(8888, 2, 4);
        // server.stop();

        // Thread thread = new Thread(() -> server.start(8888, 2, 4), "rsocket-server");
        // thread.setDaemon(false);
        // thread.start();

        System.in.read();
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
        // https://netty.io/wiki/reference-counted-objects.html
        // io.netty.util.ResourceLeakDetector
        // System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ResourceLeakDetector.setLevel(Level.ADVANCED);

        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("jsync-server"));
        TcpResources.set(LoopResources.create("jsync-server", selectCount, workerCount, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        getLogger().info("starting jsync-rsocket server on port: {}", port);

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th -> {
        });

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

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(port)
                //.runOn(LoopResources.create("jsync-server", selectCount, workerCount, false))
                .doOnUnbound(connection -> LOGGER.info("Unbound: {}", connection.channel()))
                ;
        // @formatter:on

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<Closeable> serverTransport = LocalServerTransport.create("test-local-" + port);

        SocketAcceptor socketAcceptor = SocketAcceptor.with(new JsyncRSocketHandler());

        // @formatter:off
        this.server = RSocketServer.create()
                .acceptor(socketAcceptor)
                .resume(resume)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bindNow(serverTransport)
                //.bind(serverTransport).block()
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
