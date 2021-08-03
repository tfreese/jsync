// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.rsocket.builder.RSocketBuilders;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import reactor.core.publisher.Hooks;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;

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

        System.setProperty("reactor.netty.ioSelectCount", Integer.toString(4));
        System.setProperty("reactor.netty.ioWorkerCount", Integer.toString(8));

        JsyncRSocketServer server = new JsyncRSocketServer();

        server.start(8888);
        // server.stop();

        // Thread thread = new Thread(() -> server.start(8888, 2, 4), "rsocket-server");
        // thread.setDaemon(false);
        // thread.start();

        System.in.read();
    }

    /**
     *
     */
    private CloseableChannel server;

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
        getLogger().info("starting jsync-rsocket server on port: {}", port);

        // https://netty.io/wiki/reference-counted-objects.html
        // io.netty.util.ResourceLeakDetector
        // System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ResourceLeakDetector.setLevel(Level.ADVANCED);

        // Globale Default-Resourcen.
        TcpResources.set(LoopResources.create("jsync-server"));
        // TcpResources.set(LoopResources.create("jsync-server", selectCount, workerCount, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th -> {
        });

        SocketAcceptor socketAcceptor = SocketAcceptor.with(new JsyncRSocketHandler());

        // @formatter:off
        this.server = RSocketBuilders.serverRemote()
                .socketAddress(new InetSocketAddress(port))
                .socketAcceptor(socketAcceptor)
                .resumeDefault()
                .logTcpServerBoundStatus()
                .logger(LOGGER)
                .build()
                .block()
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