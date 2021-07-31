// Created: 31.07.2021
package de.freese.jsync.rsocket.factory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import de.freese.jsync.rsocket.factory.MyRsocketServer.Transport;
import io.netty.channel.local.LocalAddress;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.local.LocalServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class MyNettyRsocketServerFactory implements MyRsocketServerFactory, MyConfigurableRSocketServerFactory
{
    /**
     *
     */
    private InetAddress address;

    /**
     *
     */
    private int fragmentSize;

    /**
     *
     */
    private Duration lifecycleTimeout;

    /**
     *
     */
    private int port = 9898;

    /**
     *
     */
    private List<Consumer<RSocketServer>> rSocketServerCustomizers = new ArrayList<>();

    /**
     *
     */
    private MyRsocketServer.Transport transport = MyRsocketServer.Transport.TCP;

    /**
     * @param rSocketServerCustomizers {@link Consumer}
     */
    public void addRSocketServerCustomizers(final Consumer<RSocketServer>...rSocketServerCustomizers)
    {
        this.rSocketServerCustomizers.addAll(Arrays.asList(rSocketServerCustomizers));
    }

    /**
     * @param server {@link RSocketServer}
     */
    private void configureServer(final RSocketServer server)
    {
        if (this.fragmentSize > 0)
        {
            server.fragment(this.fragmentSize);
        }

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(s -> System.out.println("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        server.payloadDecoder(PayloadDecoder.DEFAULT);
        server.resume(resume);

        this.rSocketServerCustomizers.forEach(customizer -> customizer.accept(server));
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyRsocketServerFactory#create(io.rsocket.SocketAcceptor)
     */
    @Override
    public MyRsocketServer create(final SocketAcceptor socketAcceptor)
    {
        ServerTransport<CloseableChannel> transport = createTransport();
        RSocketServer server = RSocketServer.create(socketAcceptor);

        configureServer(server);

        Mono<CloseableChannel> starter = server.bind(transport);

        return new MyNettyRsocketServer(starter, this.lifecycleTimeout);
    }

    /**
     * @return {@link ServerTransport}
     */
    private ServerTransport<CloseableChannel> createTransport()
    {
        if (this.transport == MyRsocketServer.Transport.WEBSOCKET)
        {
            return createTransportWebSocket();
        }
        // else if (this.transport == MyRsocketServer.Transport.LOCAL)
        // {
        // return createTransportWebSocket();
        // }

        return createTransportTcp();
    }

    /**
     * {@link LocalAddress}
     *
     * @return {@link ServerTransport}
     */
    private ServerTransport<Closeable> createTransportLocal()
    {
        return LocalServerTransport.create("server-local-" + getListenAddress().getPort());
    }

    /**
     * @return {@link ServerTransport}
     */
    private ServerTransport<CloseableChannel> createTransportTcp()
    {
        TcpServer tcpServer = TcpServer.create();

        tcpServer = tcpServer.doOnBound(connection -> System.out.println("Bound: {} " + connection.channel()));
        tcpServer = tcpServer.doOnUnbound(connection -> System.out.println("Unbound: {}" + connection.channel()));

        // if (this.resourceFactory != null) {
        // tcpServer = tcpServer.runOn(this.resourceFactory.getLoopResources());
        // }
        //
        // if (this.ssl != null && this.ssl.isEnabled()) {
        // TcpSslServerCustomizer sslServerCustomizer = new TcpSslServerCustomizer(this.ssl, this.sslStoreProvider);
        // tcpServer = sslServerCustomizer.apply(tcpServer);
        // }

        return TcpServerTransport.create(tcpServer.bindAddress(this::getListenAddress));
    }

    /**
     * @return {@link ServerTransport}
     */
    private ServerTransport<CloseableChannel> createTransportWebSocket()
    {
        HttpServer httpServer = HttpServer.create();

        // if (this.resourceFactory != null) {
        // httpServer = httpServer.runOn(this.resourceFactory.getLoopResources());
        // }
        //
        // if (this.ssl != null && this.ssl.isEnabled()) {
        // httpServer = customizeSslConfiguration(httpServer);
        // }

        return WebsocketServerTransport.create(httpServer.bindAddress(this::getListenAddress));
    }

    /**
     * @return {@link InetSocketAddress}
     */
    private InetSocketAddress getListenAddress()
    {
        if (this.address != null)
        {
            return new InetSocketAddress(this.address.getHostAddress(), this.port);
        }

        return new InetSocketAddress(this.port);
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyConfigurableRSocketServerFactory#setAddress(java.net.InetAddress)
     */
    @Override
    public void setAddress(final InetAddress address)
    {
        this.address = address;
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyConfigurableRSocketServerFactory#setFragmentSize(int)
     */
    @Override
    public void setFragmentSize(final int fragmentSize)
    {
        this.fragmentSize = fragmentSize;
    }

    /**
     * Für das Starten des Servers.
     *
     * @param lifecycleTimeout {@link Duration}
     */
    public void setLifecycleTimeout(final Duration lifecycleTimeout)
    {
        this.lifecycleTimeout = lifecycleTimeout;
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyConfigurableRSocketServerFactory#setPort(int)
     */
    @Override
    public void setPort(final int port)
    {
        this.port = port;
    }

    /**
     * @param rSocketServerCustomizers {@link Collection}
     */
    public void setRSocketServerCustomizers(final Collection<Consumer<RSocketServer>> rSocketServerCustomizers)
    {
        this.rSocketServerCustomizers = new ArrayList<>(rSocketServerCustomizers);
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyConfigurableRSocketServerFactory#setTransport(de.freese.jsync.rsocket.factory.MyRsocketServer.Transport)
     */
    @Override
    public void setTransport(final Transport transport)
    {
        this.transport = transport;
    }
}
