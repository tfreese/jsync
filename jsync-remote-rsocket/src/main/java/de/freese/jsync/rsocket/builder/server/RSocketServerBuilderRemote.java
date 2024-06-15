// Created: 15 Juni 2024
package de.freese.jsync.rsocket.builder.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.function.UnaryOperator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;

/**
 * @author Thomas Freese
 */
public final class RSocketServerBuilderRemote extends AbstractServerBuilder<RSocketServerBuilderRemote, Mono<CloseableChannel>> {

    public RSocketServerBuilderRemote addTcpServerCustomizer(final UnaryOperator<TcpServer> tcpServerCustomizer) {
        getBuilderSupport().addTcpServerCustomizer(tcpServerCustomizer);

        return this;
    }

    @Override
    public Mono<CloseableChannel> build() {
        final RSocketServer rSocketServer = getBuilderSupport().configure(RSocketServer.create());
        final TcpServer tcpServer = getBuilderSupport().configure(TcpServer.create());

        final ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);

        return rSocketServer.bind(serverTransport)
                .doOnNext(channel -> getBuilderSupport().startDaemonOnCloseThread(channel, getLogger()))
                ;
    }

    public RSocketServerBuilderRemote logTcpServerBoundStatus() {
        getBuilderSupport().logTcpServerBoundStatus(getLogger());

        return this;
    }

    public RSocketServerBuilderRemote protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        getBuilderSupport().protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketServerBuilderRemote protocolSslContextSpecCertificate() throws Exception {
        final char[] password = "password".toCharArray();

        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/server_keystore.p12")) {
            keyStore.load(is, password);
        }

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        keyManagerFactory.init(keyStore, password);

        final KeyStore trustStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/server_truststore.p12")) {
            trustStore.load(is, password);
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        final Certificate certificate = keyStore.getCertificate("myServer");
        final PrivateKey privateKey = (PrivateKey) keyStore.getKey("myServer", password);
        // KeyStore.Entry entry = keyStore.getEntry("myServer", new KeyStore.PasswordProtection(password));
        // PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        getBuilderSupport().protocolSslContextSpecCertificate(keyManagerFactory, trustManagerFactory, (X509Certificate) certificate, privateKey);

        return this;
    }

    public RSocketServerBuilderRemote protocolSslContextSpecCertificateSelfSigned() throws Exception {
        getBuilderSupport().protocolSslContextSpecCertificateSelfSigned();

        return this;
    }

    public RSocketServerBuilderRemote resume(final Resume resume) {
        getBuilderSupport().resume(resume);

        return this;
    }

    public RSocketServerBuilderRemote resumeDefault() {
        getBuilderSupport().resumeDefault(getLogger());

        return this;
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    public RSocketServerBuilderRemote runOn(final EventLoopGroup eventLoopGroup) {
        getBuilderSupport().runOn(eventLoopGroup);

        return this;
    }

    public RSocketServerBuilderRemote runOn(final LoopResources loopResources) {
        getBuilderSupport().runOn(loopResources);

        return this;
    }

    public RSocketServerBuilderRemote socketAddress(final SocketAddress socketAddress) {
        getBuilderSupport().socketAddress(socketAddress);

        return this;
    }

    @Override
    protected RSocketServerBuilderRemote self() {
        return this;
    }
}
