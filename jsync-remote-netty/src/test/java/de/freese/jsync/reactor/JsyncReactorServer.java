// Created: 27.09.2020
package de.freese.jsync.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelOption;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

/**
 * @author Thomas Freese
 */
public class JsyncReactorServer
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncReactorServer.class);

    /**
     *
     */
    private DisposableServer disposableServer;

    /**
    *
    */
    private String name = getClass().getSimpleName();

    /**
     * Erstellt ein neues {@link JsyncReactorServer} Object.
     */
    public JsyncReactorServer()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param port int
     * @param acceptorThreads int
     * @param workerThreads int
     * @throws Exception Falls was schief geht.
     */
    public void start(final int port, final int acceptorThreads, final int workerThreads) throws Exception
    {
        getLogger().info("starting {}", this.name);

        LoopResources loop = LoopResources.create("server", acceptorThreads, workerThreads, true);

        // @formatter:off
        TcpServer tcpServer = TcpServer
                .create()
                .runOn(loop, true)
                .port(port)
                //.wiretap(true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .doOnBound(server ->  getLogger().info("{} started", this.name))
                .handle(new JsyncReactorServerHandler())
//                .bindNow()
                ;
        // @formatter:on

        // SelfSignedCertificate ssc = new SelfSignedCertificate();
        // tcpServer = tcpServer.secure(spec -> spec.sslContext(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())));

        this.disposableServer = tcpServer.bindNow();

        // disposableServer.onDispose().block();
    }

    /**
    *
    */
    public void stop()
    {
        getLogger().info("stopping {}", this.name);

        this.disposableServer.disposeNow();

        getLogger().info("{} stopped", this.name);
    }
}
