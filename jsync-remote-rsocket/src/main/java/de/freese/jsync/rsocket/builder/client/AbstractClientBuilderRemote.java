// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.core.Resume;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
abstract class AbstractClientBuilderRemote<T> extends AbstractClientBuilder<T> {

    public T addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        getBuilderSupport().addTcpClientCustomizer(tcpClientCustomizer);

        return self();
    }

    public T logTcpClientBoundStatus() {
        getBuilderSupport().logTcpClientBoundStatus(getLogger());

        return self();
    }

    public T protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        getBuilderSupport().protocolSslContextSpec(protocolSslContextSpec);

        return self();
    }

    public T protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        getBuilderSupport().protocolSslContextSpecCertificate(trustManagerFactory);

        return self();
    }

    public T protocolSslContextSpecTrusted() {
        getBuilderSupport().protocolSslContextSpecTrusted();

        return self();
    }

    public T resume(final Resume resume) {
        getBuilderSupport().resume(resume);

        return self();
    }

    public T resumeDefault() {
        getBuilderSupport().resumeDefault(getLogger());

        return self();
    }

    public T retry(final Retry retry) {
        getBuilderSupport().retry(retry);

        return self();
    }

    public T retryDefault() {
        getBuilderSupport().retryDefault(getLogger());

        return self();
    }

    public T runOn(final EventLoopGroup eventLoopGroup) {
        getBuilderSupport().runOn(eventLoopGroup);

        return self();
    }

    public T runOn(final LoopResources loopResources) {
        getBuilderSupport().runOn(loopResources);

        return self();
    }
}
