// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
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
     * @param serverAddresses {@link List}
     * @return {@link List}
     */
    static List<Disposable> startServer(final List<SocketAddress> serverAddresses)
    {
        List<Disposable> servers = new ArrayList<>();

        for (SocketAddress serverAddress : serverAddresses)
        {
            Server server = new Server();

            try
            {
                server.start(serverAddress);

                servers.add(server);
            }
            catch (Exception ex)
            {
                LOGGER.error(null, ex);
            }
        }

        return servers;
    }

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
     * @param serverAddress {@link SocketAddress}
     * @throws Exception Falls was schief geht
     */
    public void start(final SocketAddress serverAddress) throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_keystore.p12"))
        {
            keyStore.load(is, "password".toCharArray());
        }

        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        // keyManagerFactory.init(keyStore, "gehaim".toCharArray());

        KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_truststore.p12"))
        {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        Certificate certificate = keyStore.getCertificate("server");
        PrivateKey privateKey = (PrivateKey) keyStore.getKey("server", "password".toCharArray());
        // KeyStore.Entry entry = keyStore.getEntry("server", new KeyStore.PasswordProtection("password".toCharArray()));
        // PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        // @formatter:off
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(privateKey, (X509Certificate) certificate)
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory) // Verursacht Fehler
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                )
                ;
        // @formatter:on

        // SelfSignedCertificate cert = new SelfSignedCertificate();
        //
//         // @formatter:off
//         ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
//                 .configure(builder -> builder
//                         .protocols("TLSv1.3")
//                         .sslProvider(SslProvider.JDK)
//                 )
//                 ;
//         // @formatter:on

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                //.host(inetSocketAddress.getHostName()).port(inetSocketAddress.getPort())
                .bindAddress(() -> serverAddress)
                .secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                .doOnUnbound(connection -> LOGGER.info("Unbound: {}", connection.channel()))
                //.runOn(LoopResources.create("server-" + port, 2, 4, true), false)
                ;
        // @formatter:on

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<Closeable> serverTransport = LocalServerTransport.create("test-local-" + port);

        SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload -> {
            String request = payload.getDataUtf8();
            LOGGER.info("Server:{} got request {}", serverAddress, request);
            return Mono.just(DefaultPayload.create("Client of Server:" + serverAddress + " response=" + request)).delayElement(Duration.ofMillis(300));
        });

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(100))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

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