// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.net.InetSocketAddress;

import de.freese.jsync.rsocket.JSyncRSocketHandlerByteBuf;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;

/**
 * @author Thomas Freese
 */
public final class JSyncRSocketServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncRSocketServer.class);

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schiefgeht.
     */
    public static void main(final String[] args) throws Exception
    {
        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        System.setProperty("reactor.netty.ioSelectCount", Integer.toString(4));
        System.setProperty("reactor.netty.ioWorkerCount", Integer.toString(8));

        int port = Integer.parseInt(args[0]);

        JSyncRSocketServer server = new JSyncRSocketServer();

        server.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-stop"));

        // server.stop();

        // Thread thread = new Thread(() -> server.start(8888), "rSocket-server");
        // thread.setDaemon(false);
        // thread.start();

        // System.in.read();
    }

    /**
     *
     */
    private CloseableChannel server;

    /**
     * @param port int
     */
    public void start(final int port)
    {
        getLogger().info("starting jSync-rSocket server on port: {}", port);

        // https://netty.io/wiki/reference-counted-objects.html
        // io.netty.util.ResourceLeakDetector
        // System.setProperty("io.netty.leakDetection.level", "PARANOID");
        ResourceLeakDetector.setLevel(Level.ADVANCED);

        // Globale Default-Ressourcen.
        TcpResources.set(LoopResources.create("server"));
        // TcpResources.set(LoopResources.create("server", selectCount, workerCount, true));
        // TcpResources.set(ConnectionProvider.create("connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gib es mehrere Logs-Meldungen !!!
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        // Hooks.onErrorDropped(th -> {
        // });

        // @formatter:off
        this.server = RSocketBuilders.serverRemote()
                .socketAddress(new InetSocketAddress(port))
                .socketAcceptor(SocketAcceptor.with(new JSyncRSocketHandlerByteBuf()))
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
        getLogger().info("stopping jSync-rSocket server");

        this.server.dispose();

        ByteBufferPool.DEFAULT.clear();
    }

    /**
     * @return {@link Logger}
     */
    Logger getLogger()
    {
        return LOGGER;
    }
}
