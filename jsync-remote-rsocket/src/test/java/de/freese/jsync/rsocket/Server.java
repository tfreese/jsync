// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
final class Server implements Disposable
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     *
     */
    private Closeable channel;

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
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(10, Duration.ofSeconds(1))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        SelfSignedCertificate cert = new SelfSignedCertificate();
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey());

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(port)
                .secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                .doOnUnbound(connection -> LOGGER.info("Unbound: {}", connection.channel()))
                //.runOn(LoopResources.create("server-" + port, 2, 4, true), false)
                ;
        // @formatter:on

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<Closeable> serverTransport = LocalServerTransport.create("test-local-" + port);

        SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload -> {
            String request = payload.getDataUtf8();
            LOGGER.info("Server:{} got request {}", port, request);
            return Mono.just(DefaultPayload.create("Client of Server:" + port + " response=" + request)).delayElement(Duration.ofMillis(100));
        });

        // @formatter:off
        this.channel = RSocketServer.create()
            .acceptor(socketAcceptor)
            //.payloadDecoder(PayloadDecoder.DEFAULT)
            .resume(resume)
            .bindNow(serverTransport)
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