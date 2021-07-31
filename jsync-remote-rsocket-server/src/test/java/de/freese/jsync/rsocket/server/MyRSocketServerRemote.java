// Created: 11.07.2021
package de.freese.jsync.rsocket.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * {@link RSocketServer} für Verwendung innerhalb einer Runtime.
 *
 * @author Thomas Freese
 */
public class MyRSocketServerRemote implements MyRSocketServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRSocketServerRemote.class);

    /**
     * @return {@link ProtocolSslContextSpec}
     *
     * @throws Exception Falls was schief geht.
     */
    static ProtocolSslContextSpec createProtocolSslContextSpec() throws Exception
    {
        // return createProtocolSslContextSpecCertificate();
        // return createProtocolSslContextSpecSelfSigned();
        return null;
    }

    /**
     * @return {@link ProtocolSslContextSpec}
     *
     * @throws Exception Falls was schief geht.
     */
    static ProtocolSslContextSpec createProtocolSslContextSpecCertificate() throws Exception
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
        return TcpSslContextSpec.forServer(privateKey, (X509Certificate) certificate)
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory) // Verursacht Fehler
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on
    }

    /**
     * @return {@link ProtocolSslContextSpec}
     *
     * @throws Exception Falls was schief geht.
     */
    static ProtocolSslContextSpec createProtocolSslContextSpecSelfSigned() throws Exception
    {
        SelfSignedCertificate cert = new SelfSignedCertificate();

        // @formatter:off
        return TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
                .configure(builder -> builder
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                 ;
         // @formatter:on
    }

    /**
     * @param socketAcceptor {@link SocketAcceptor}
     * @param serverTransport {@link ServerTransport}
     *
     * @return {@link CloseableChannel}
     */
    static CloseableChannel createRSocketServer(final SocketAcceptor socketAcceptor, final ServerTransport<CloseableChannel> serverTransport)
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        return RSocketServer.create()
                .acceptor(socketAcceptor)
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .resume(resume)
                .bindNow(serverTransport)
                ;
        // @formatter:on
    }

    /**
     * @param serverAddress {@link SocketAddress}
     * @param protocolSslContextSpec {@link ProtocolSslContextSpec}
     *
     * @return {@link RSocketClient}
     */
    static TcpServer createTcpServer(final SocketAddress serverAddress, final ProtocolSslContextSpec protocolSslContextSpec)
    {
        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                //.host(getHost()).port(getPort())
                .bindAddress(() -> serverAddress)
                .doOnBound(connection -> {
                    LOGGER.info("Bound: {}", connection.channel());
                    })
                .doOnUnbound(connection -> LOGGER.info("Unbound: {}", connection.channel()))
                //.runOn(new NioEventLoopGroup(4, new JsyncThreadFactory("server-" + ((InetSocketAddress) serverAddress).getPort() + "-")))
                // EpollEventLoopGroup geht nur auf Linux
                ;
        // @formatter:on

        if (protocolSslContextSpec != null)
        {
            tcpServer = tcpServer.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec));
        }

        return tcpServer;
    }

    /**
     *
     */
    private CloseableChannel server;

    /**
     *
     */
    private final Function<Integer, SocketAcceptor> socketAcceptor;

    /**
     *
     */
    private final InetSocketAddress socketAddress;

    /**
     * Erstellt ein neues {@link MyRSocketServerRemote} Object.
     *
     * @param socketAddress {@link InetSocketAddress}
     * @param socketAcceptor {@link SocketAcceptor}
     */
    public MyRSocketServerRemote(final InetSocketAddress socketAddress, final Function<Integer, SocketAcceptor> socketAcceptor)
    {
        super();

        this.socketAddress = Objects.requireNonNull(socketAddress, "socketAddress required");
        this.socketAcceptor = Objects.requireNonNull(socketAcceptor, "socketAcceptor required");
    }

    /**
     * Erstellt ein neues {@link MyRSocketServerRemote} Object.
     *
     * @param host String
     * @param port int
     * @param socketAcceptor {@link SocketAcceptor}
     */
    public MyRSocketServerRemote(final String host, final int port, final Function<Integer, SocketAcceptor> socketAcceptor)
    {
        this(new InetSocketAddress(host, port), socketAcceptor);
    }

    /**
     * @return {@link InetSocketAddress}
     */
    public InetSocketAddress getSocketAddress()
    {
        // return this.server.address();
        return this.socketAddress;
    }

    /**
     * @see de.freese.jsync.rsocket.server.MyRSocketServer#start()
     */
    @Override
    public void start() throws Exception
    {
        ProtocolSslContextSpec protocolSslContextSpec = createProtocolSslContextSpec();

        TcpServer tcpServer = createTcpServer(this.socketAddress, protocolSslContextSpec);

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<Closeable> serverTransport = LocalServerTransport.create("test-local-" + getSocketAddress().getPort());

        this.server = createRSocketServer(this.socketAcceptor.apply(this.socketAddress.getPort()), serverTransport);
    }

    /**
     * @see de.freese.jsync.rsocket.server.MyRSocketServer#stop()
     */
    @Override
    public void stop()
    {
        this.server.dispose();
    }
}
