// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketSimpleDemo
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketSimpleDemo.class);

    /**
     * @param port int
     * @return {@link RSocket}
     * @throws Exception Falls was schief geht
     */
    private static final RSocket createClient(final int port) throws Exception
    {
       // @formatter:off
       SslContextBuilder sslContextBuilderClient = SslContextBuilder.forClient()
               .trustManager(InsecureTrustManagerFactory.INSTANCE)
               .sslProvider(SslProvider.JDK)
               ;
       // @formatter:on

       // @formatter:off
       TcpClient tcpClient = TcpClient.create()
               .host("localhost")
               .port(port)
               .runOn(LoopResources.create("client-" + port, 2, true))
               .secure(sslContextSpec -> sslContextSpec.sslContext(sslContextBuilderClient))
               ;
       // @formatter:on

       // @formatter:off
       RSocket client = RSocketConnector.create()
           //.payloadDecoder(PayloadDecoder.ZERO_COPY)
           .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
           //.reconnect(Retry.backoff(50, Duration.ofMillis(500)))
           .connect(TcpClientTransport.create(tcpClient))
           //.connect(LocalClientTransport.create("test-local-" + port))
           .block()
           ;
       // @formatter:on

        return client;
    }

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht
     */
    public static void main(final String[] args) throws Exception
    {
        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("demo", 4, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));

        int port = 7000;

        CloseableChannel server = startServer(port);
        RSocket client = createClient(port);

        // Request - Response
        for (int i = 0; i < 30; i++)
        {
           // @formatter:off
           client
               .requestResponse(DefaultPayload.create("test " + i))
               .map(Payload::getDataUtf8)
               .doOnNext(LOGGER::info)
               .block() // Wartet auf den Response.
               //.subscribe() // Führt alles im Hintergrund aus.
               ;
           // @formatter:on
        }

        // System.err.println(((PooledByteBufAllocator) ByteBufAllocator.DEFAULT).dumpStats());

        client.dispose();
        server.dispose();
    }

    /**
     * @param port int
     * @return {@link CloseableChannel}
     * @throws Exception Falls was schief geht
     */
    private static final CloseableChannel startServer(final int port) throws Exception
    {
       // @formatter:off
       Resume resume = new Resume()
               .sessionDuration(Duration.ofMinutes(5))
               .retry(
                       Retry
                           .fixedDelay(20, Duration.ofSeconds(1))
                           .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
               )
               ;
       // @formatter:on

        SelfSignedCertificate cert = new SelfSignedCertificate();
        SslContextBuilder sslContextBuilderServer = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

       // @formatter:off
       TcpServer tcpServer = TcpServer.create()
               .host("localhost")
               .port(port)
               .runOn(LoopResources.create("server-" + port, 1, 2, true))
               .secure(sslContextSpec -> sslContextSpec.sslContext(sslContextBuilderServer))
               ;
       // @formatter:on

        SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload -> {
            LOGGER.info("Server {} got {}", port, payload.getDataUtf8());
            return Mono.just(DefaultPayload.create("Server " + port + " response")).delayElement(Duration.ofMillis(100));
        });

       // @formatter:off
       CloseableChannel server = RSocketServer.create()
           .acceptor(socketAcceptor)
           //.payloadDecoder(PayloadDecoder.ZERO_COPY)
           .resume(resume)
           .bindNow(TcpServerTransport.create(tcpServer))
           //.bind(LocalServerTransport.create("test-local-" + port))
           //.block()
           ;
       // @formatter:on

        return server;
    }

    /**
     * Erzeugt ein neues {@link RSocketSimpleDemo} Objekt.
     */
    private RSocketSimpleDemo()
    {
        super();
    }
}