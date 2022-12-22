// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.core.Resume;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRSocketClientRemoteBuilder<T extends AbstractRSocketClientRemoteBuilder<?>>
        extends AbstractRSocketClientBuilder<AbstractRSocketClientRemoteBuilder<T>>
{
    private final List<UnaryOperator<TcpClient>> tcpClientCustomizers = new ArrayList<>();

    public T addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer)
    {
        this.tcpClientCustomizers.add(Objects.requireNonNull(tcpClientCustomizer, "tcpClientCustomizer required"));

        return (T) this;
    }

    public T logTcpClientBoundStatus()
    {
        // @formatter:off
        addTcpClientCustomizer(tcpClient -> tcpClient
            .doOnConnected(connection -> getLogger().info("Connected: {}", connection.channel()))
            .doOnDisconnected(connection -> getLogger().info("Disconnected: {}", connection.channel()))
            )
            ;
        // @formatter:on

        return (T) this;
    }

    public T protocolSslContextSpec(final ProtocolSslContextSpec protocolSslContextSpec)
    {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        addTcpClientCustomizer(tcpClient -> tcpClient.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));

        return (T) this;
    }

    public T protocolSslContextSpecCertificate() throws Exception
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
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on

        protocolSslContextSpec(protocolSslContextSpec);

        return (T) this;
    }

    public T protocolSslContextSpecTrusted()
    {
        // @formatter:off
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on

        protocolSslContextSpec(protocolSslContextSpec);

        return (T) this;
    }

    public T resume(final Resume resume)
    {
        Objects.requireNonNull(resume, "resume required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.resume(resume));

        return (T) this;
    }

    public T resumeDefault()
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> getLogger().info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        return resume(resume);
    }

    public T retry(final Retry retry)
    {
        Objects.requireNonNull(retry, "retry required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.reconnect(retry));

        return (T) this;
    }

    public T retryDefault()
    {
        return retry(Retry.fixedDelay(5, Duration.ofMillis(100)));
        // return retry(Retry.backoff(50, Duration.ofMillis(100))));
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    public T runOn(final EventLoopGroup eventLoopGroup)
    {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        addTcpClientCustomizer(tcpClient -> tcpClient.runOn(eventLoopGroup));

        return (T) this;
    }

    public T runOn(final LoopResources loopResources)
    {
        Objects.requireNonNull(loopResources, "loopResources required");

        addTcpClientCustomizer(tcpClient -> tcpClient.runOn(loopResources));

        return (T) this;
    }

    protected TcpClient configure(final TcpClient tcpClient)
    {
        TcpClient client = tcpClient;

        for (UnaryOperator<TcpClient> clientCustomizer : this.tcpClientCustomizers)
        {
            client = clientCustomizer.apply(client);
        }

        return client;
    }
}
