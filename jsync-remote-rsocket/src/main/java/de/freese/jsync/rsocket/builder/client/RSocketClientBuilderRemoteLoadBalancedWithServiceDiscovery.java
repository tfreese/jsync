// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery.class);

    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();

    private Supplier<List<SocketAddress>> serviceDiscovery;

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        builderSupport.addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        builderSupport.addTcpClientCustomizer(tcpClientCustomizer);

        return this;
    }

    public RSocketClient build() {
        final Publisher<List<LoadbalanceTarget>> serverProducer = Mono.fromSupplier(this.serviceDiscovery)
                .map(servers -> {
                    LOGGER.info("Update Server Instances: {}", servers);

                    return servers.stream()
                            .map(serverAddress -> {
                                final TcpClient tcpClient = builderSupport.configure(TcpClient.create()).remoteAddress(() -> serverAddress);
                                final ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

                                return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
                            })
                            .toList()
                            ;
                })
                .repeatWhen(flux -> flux.delayElements(Duration.ofMillis(600))) // Flux regelmäßig aktualisieren.
                ;

        final RSocketConnector rSocketConnector = builderSupport.configure(RSocketConnector.create());

        return LoadbalanceRSocketClient.builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                // .weightedLoadbalanceStrategy()
                .build();
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery logTcpClientBoundStatus() {
        builderSupport.logTcpClientBoundStatus(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        builderSupport.protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        builderSupport.protocolSslContextSpecCertificate(trustManagerFactory);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery protocolSslContextSpecTrusted() {
        builderSupport.protocolSslContextSpecTrusted();

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery resume(final Resume resume) {
        builderSupport.resume(resume);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery resumeDefault() {
        builderSupport.resumeDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery retry(final Retry retry) {
        builderSupport.retry(retry);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery retryDefault() {
        builderSupport.retryDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery runOn(final EventLoopGroup eventLoopGroup) {
        builderSupport.runOn(eventLoopGroup);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery runOn(final LoopResources loopResources) {
        builderSupport.runOn(loopResources);

        return this;
    }

    /**
     * Simulate Service-Discovery.<br>
     * org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons
     */
    public RSocketClientBuilderRemoteLoadBalancedWithServiceDiscovery serviceDiscovery(final Supplier<List<SocketAddress>> serviceDiscovery) {
        this.serviceDiscovery = Objects.requireNonNull(serviceDiscovery, "serviceDiscovery required");

        return this;
    }
}
