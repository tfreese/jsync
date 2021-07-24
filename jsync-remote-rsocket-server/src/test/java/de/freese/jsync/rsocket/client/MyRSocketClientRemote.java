// Created: 11.07.2021
package de.freese.jsync.rsocket.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.time.Duration;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class MyRSocketClientRemote implements MyRSocketClient<InetSocketAddress>
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRSocketClientRemote.class);

    /**
     * @return {@link ProtocolSslContextSpec}
     * @throws Exception Falls was schief geht.
     */
    static ProtocolSslContextSpec createProtocolSslContextSpec() throws Exception
    {
        // return createProtocolSslContextSpecCertificate();
        // return createProtocolSslContextSpecTrusted();

        return null;
    }

    /**
     * @return {@link ProtocolSslContextSpec}
     * @throws Exception Falls was schief geht.
     */
    static ProtocolSslContextSpec createProtocolSslContextSpecCertificate() throws Exception
    {
        // KeyStore keyStore = KeyStore.getInstance("PKCS12");
        //
        // try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/client_keystore.p12"))
        // {
        // keyStore.load(is, "password".toCharArray());
        // }
        //
        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        // keyManagerFactory.init(keyStore, "gehaim".toCharArray());

        KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/client_truststore.p12"))
        {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        // @formatter:off
        return TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        // .keyManager(keyManagerFactory)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on
    }

    /**
     * @return {@link ProtocolSslContextSpec}
     */
    static ProtocolSslContextSpec createProtocolSslContextSpecTrusted()
    {
        // @formatter:off
        return TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on
    }

    /**
     * @return {@link RSocketConnector}
     */
    static RSocketConnector createRSocketConnector()
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(100))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        return RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .reconnect(Retry.fixedDelay(5, Duration.ofMillis(100)))
                // .reconnect(Retry.backoff(50, Duration.ofMillis(100)))
                .resume(resume)
                ;
        // @formatter:on
    }

    /**
     * @param serverAddress {@link SocketAddress}
     * @param protocolSslContextSpec {@link ProtocolSslContextSpec}
     * @return {@link TcpClient}
     */
    static TcpClient createTcpClient(final SocketAddress serverAddress, final ProtocolSslContextSpec protocolSslContextSpec)
    {
        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                //.host(serverAddress.getHostName()).port(serverAddress.getPort())
                .remoteAddress(() -> serverAddress)
                .doOnConnected(connection -> LOGGER.info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel()))
                //.runOn(new NioEventLoopGroup(4, new JsyncThreadFactory("client-" + ((InetSocketAddress) serverAddress).getPort() + "-")))
                // EpollEventLoopGroup geht nur auf Linux
                ;
        // @formatter:on

        if (protocolSslContextSpec != null)
        {
            tcpClient = tcpClient.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec));
        }

        return tcpClient;
    }

    /**
    *
    */
    private RSocketClient client;

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#connect(java.lang.Object)
     */
    @Override
    public void connect(final InetSocketAddress serverInfo) throws Exception
    {
        ProtocolSslContextSpec protocolSslContextSpec = createProtocolSslContextSpec();

        TcpClient tcpClient = createTcpClient(serverInfo, protocolSslContextSpec);

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

        RSocketConnector rSocketConnector = createRSocketConnector();

        Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        this.client = RSocketClient.from(rSocket);
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#disconnect()
     */
    @Override
    public void disconnect()
    {
        getClient().dispose();
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#getClient()
     */
    @Override
    public RSocketClient getClient()
    {
        return this.client;
    }
}
