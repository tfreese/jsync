// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
abstract class AbstractClientBuilderRemote<T> {
    private final RSocketClientBuilderSupport builderSupport = new RSocketClientBuilderSupport();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public T addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        builderSupport.addRSocketConnectorCustomizer(rSocketConnectorCustomizer);

        return self();
    }

    public T addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        builderSupport.addTcpClientCustomizer(tcpClientCustomizer);

        return self();
    }

    public T logTcpClientBoundStatus() {
        builderSupport.logTcpClientBoundStatus(getLogger());

        return self();
    }

    public T payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return self();
    }

    public T protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        builderSupport.protocolSslContextSpec(protocolSslContextSpec);

        return self();
    }

    public T protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        builderSupport.protocolSslContextSpecCertificate(trustManagerFactory);

        return self();
    }

    public T protocolSslContextSpecTrusted() {
        builderSupport.protocolSslContextSpecTrusted();

        return self();
    }

    public T remoteAddress(final SocketAddress remoteAddress) {
        builderSupport.remoteAddress(remoteAddress);

        return self();
    }

    public T resume(final Resume resume) {
        builderSupport.resume(resume);

        return self();
    }

    public T resumeDefault() {
        builderSupport.resumeDefault(getLogger());

        return self();
    }

    public T retry(final Retry retry) {
        builderSupport.retry(retry);

        return self();
    }

    public T retryDefault() {
        builderSupport.retryDefault(getLogger());

        return self();
    }

    public T runOn(final EventLoopGroup eventLoopGroup) {
        builderSupport.runOn(eventLoopGroup);

        return self();
    }

    public T runOn(final LoopResources loopResources) {
        builderSupport.runOn(loopResources);

        return self();
    }

    protected RSocketClientBuilderSupport getBuilderSupport() {
        return builderSupport;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected abstract T self();
}
