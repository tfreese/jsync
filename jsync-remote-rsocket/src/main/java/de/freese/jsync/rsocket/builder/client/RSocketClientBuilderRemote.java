// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
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
public class RSocketClientBuilderRemote {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClientBuilderRemote.class);

    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();

    public RSocketClientBuilderRemote addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        builderSupport.addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return this;
    }

    public RSocketClientBuilderRemote addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        builderSupport.addTcpClientCustomizer(tcpClientCustomizer);

        return this;
    }

    public RSocketClient build() {
        final TcpClient tcpClient = builderSupport.configure(TcpClient.create());
        final RSocketConnector rSocketConnector = builderSupport.configure(RSocketConnector.create());

        final ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

        final Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    public RSocketClientBuilderRemote logTcpClientBoundStatus() {
        builderSupport.logTcpClientBoundStatus(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemote payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }

    public RSocketClientBuilderRemote protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        builderSupport.protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketClientBuilderRemote protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        builderSupport.protocolSslContextSpecCertificate(trustManagerFactory);

        return this;
    }

    public RSocketClientBuilderRemote protocolSslContextSpecTrusted() {
        builderSupport.protocolSslContextSpecTrusted();

        return this;
    }

    public RSocketClientBuilderRemote remoteAddress(final SocketAddress remoteAddress) {
        builderSupport.remoteAddress(remoteAddress);

        return this;
    }

    public RSocketClientBuilderRemote resume(final Resume resume) {
        builderSupport.resume(resume);

        return this;
    }

    public RSocketClientBuilderRemote resumeDefault() {
        builderSupport.resumeDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemote retry(final Retry retry) {
        builderSupport.retry(retry);

        return this;
    }

    public RSocketClientBuilderRemote retryDefault() {
        builderSupport.retryDefault(LOGGER);

        return this;
    }

    public RSocketClientBuilderRemote runOn(final EventLoopGroup eventLoopGroup) {
        builderSupport.runOn(eventLoopGroup);

        return this;
    }

    public RSocketClientBuilderRemote runOn(final LoopResources loopResources) {
        builderSupport.runOn(loopResources);

        return this;
    }
}
