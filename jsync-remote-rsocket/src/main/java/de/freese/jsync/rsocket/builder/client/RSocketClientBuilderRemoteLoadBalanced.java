// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import reactor.core.publisher.Flux;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketClientBuilderRemoteLoadBalanced {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClientBuilderRemoteLoadBalanced.class);

    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();
    private final List<SocketAddress> remoteAddresses = new ArrayList<>();

    public RSocketClientBuilderRemoteLoadBalanced addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        builderSupport.addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        builderSupport.addTcpClientCustomizer(tcpClientCustomizer);

        return this;
    }

    public RSocketClient build() {
        final Publisher<List<LoadbalanceTarget>> serverProducer = Flux.fromIterable(this.remoteAddresses)
                .map(serverAddress -> {
                    final TcpClient tcpClient = builderSupport.configure(TcpClient.create()).remoteAddress(() -> serverAddress);
                    final ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

                    return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
                })
                .collectList();

        // Publisher<List<LoadbalanceTarget>> serverProducer2 = Flux.interval(Duration.ofSeconds(1)).log().map(i -> {
        // int val = i.intValue();
        //
        // return switch (val) {
        // case 0 -> Collections.emptyList();
        // case 1 -> List.of(targets.get(0));
        // case 2 -> List.of(targets.get(0), targets.get(1));
        // case 3 -> List.of(targets.get(0), targets.get(2));
        // case 4 -> List.of(targets.get(1), targets.get(2));
        // case 5 -> List.of(targets.get(0), targets.get(1), targets.get(2));
        // case 6 -> Collections.emptyList();
        // case 7 -> Collections.emptyList();
        // default -> List.of(targets.get(0), targets.get(1), targets.get(2));
        // };
        // });

        final RSocketConnector rSocketConnector = builderSupport.configure(RSocketConnector.create());

        return LoadbalanceRSocketClient.builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                // .weightedLoadbalanceStrategy()
                .build()
                ;
    }

    public RSocketClientBuilderRemoteLoadBalanced logTcpClientBoundStatus() {
        builderSupport.logTcpClientBoundStatus(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        builderSupport.protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        builderSupport.protocolSslContextSpecCertificate(trustManagerFactory);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced protocolSslContextSpecTrusted() {
        builderSupport.protocolSslContextSpecTrusted();

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced remoteAddresses(final List<? extends SocketAddress> remoteAddresses) {
        Objects.requireNonNull(remoteAddresses, "remoteAddresses required");

        this.remoteAddresses.addAll(remoteAddresses);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced resume(final Resume resume) {
        builderSupport.resume(resume);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced resumeDefault() {
        builderSupport.resumeDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced retry(final Retry retry) {
        builderSupport.retry(retry);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced retryDefault() {
        builderSupport.retryDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced runOn(final EventLoopGroup eventLoopGroup) {
        builderSupport.runOn(eventLoopGroup);

        return this;
    }

    public RSocketClientBuilderRemoteLoadBalanced runOn(final LoopResources loopResources) {
        builderSupport.runOn(loopResources);

        return this;
    }
}
