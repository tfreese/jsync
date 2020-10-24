// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class MyRSocketDemo
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRSocketDemo.class);

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        // Fehlermeldung, wenn Client die Verbindung schliesst.
        Hooks.onErrorDropped(th -> LOGGER.error(th.getMessage()));

        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("demo", 4, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        List<Disposable> servers = startServers(7000, 8000, 9000);

        Mono<RSocket> client = startClients(7000, 8000, 9000);

        // Request - Response
        for (int i = 0; i < 30; i++)
        {
            // if (i == 10)
            // {
            // // Funktioniert irgendwie nicht.
            // Disposable server = servers.remove(0);
            // server.dispose();
            // }

            // @formatter:off
            client.block()
                .requestResponse(DefaultPayload.create("test " + i))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                .doOnError(th -> LOGGER.error(null, th))
                .block() // Wartet auf jeden Response.
                //.subscribe() // Führt alles im Hintergrund aus.
                ;
            // @formatter:on
        }

        // System.err.println(((PooledByteBufAllocator) ByteBufAllocator.DEFAULT).dumpStats());

        // // Channel: Erwartung = '/tmp/test.txt' mit 'Hello World !' wird geschrieben.
        // Payload meta = ByteBufPayload.create("", "/tmp/test.txt");
        // Payload first = ByteBufPayload.create("Hello");
        // Payload second = ByteBufPayload.create(" World !");
        // Flux<Payload> fileFlux = Flux.just(first, second);
        //
//        // @formatter:off
//        socket
//            .requestChannel(Flux.concat(Mono.just(meta),fileFlux).doOnEach(signal -> RSocketUtils.release(signal.get())))
//            .map(payload -> {
//                String response = payload.getDataUtf8();
//                RSocketUtils.release(payload);
//                return response;
//            })
//            .doOnNext(LOGGER::debug)
//            .doOnError(th -> LOGGER.error(null, th))
//            .then()
//            .block()
//            ;
//        // @formatter:on

        // ReadableByteChannel readableByteChannel = Files.newByteChannel(path, StandardOpenOption.READ);
        // fileFlux = Flux.generate(new ReadableByteChannelGenerator(readableByteChannel, 1024 * 1024 * 4)).map(ByteBufPayload::create).doFinally(signalType ->
        // {
        // try
        // {
        // readableByteChannel.close();
        // }
        // catch (IOException ex)
        // {
        // LOGGER.error(ex.getMessage());
        //
        // throw new UncheckedIOException(ex);
        // }
        // });

        // fileFlux = Flux.using(() -> readableByteChannel,
        // channel -> Flux.generate(new ReadableByteChannelGenerator(channel, 1024 * 1024 * 4)).map(ByteBufPayload::create), channel -> {
        // try
        // {
        // channel.close();
        // }
        // catch (IOException ex)
        // {
        // throw new UncheckedIOException(ex);
        // }
        // });

        servers.forEach(Disposable::dispose);
        client.block().dispose();
    }

    /**
     * @param ports int[]
     * @return {@link Mono}
     * @throws Exception Falls was schief geht.
     */
    private static Mono<RSocket> startClients(final int...ports) throws Exception
    {
        // @formatter:off
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(SslProvider.JDK)
                ;
       // @formatter:on

        // @formatter:off
        List<RSocketSupplier> rSocketSupplierList = IntStream.of(ports)
                .mapToObj(port ->
                     RSocketConnector
                            .create()
                            //.payloadDecoder(PayloadDecoder.ZERO_COPY)
                            .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                            //.reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                            .connect(TcpClientTransport.create(TcpClient.create()
                                        .host("localhost")
                                        .port(port)
                                        .runOn(LoopResources.create("client-" + port, 2, true))
                                        .secure(sslContextSpec -> sslContextSpec.sslContext(sslContextBuilder))
                                        )
                                    )
                            //.connect(LocalClientTransport.create("test-local-" + port))
                            .block()
                )
                .map(rsocket -> new RSocketSupplier(() -> Mono.just(rsocket)))
                .collect(Collectors.toList())
                ;
        // @formatter:on

        Publisher<List<RSocketSupplier>> factories = subscriber -> {
            subscriber.onNext(rSocketSupplierList);
            subscriber.onComplete();
        };

        LoadBalancedRSocketMono balancer = LoadBalancedRSocketMono.create(factories);

        return balancer;

//        // @formatter:off
//        Mono<RSocket> client = RSocketConnector
//                .create()
//                //.payloadDecoder(PayloadDecoder.ZERO_COPY)
//                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
//                //.reconnect(Retry.backoff(50, Duration.ofMillis(500)))
//                .connect(TcpClientTransport.create(TcpClient.create()
//                            .host("localhost")
//                            .port(ports[0])
//                            .runOn(LoopResources.create("client-" + ports[0], 2, true))
//                            )
//                        )
//                //.connect(LocalClientTransport.create("test-local-" + port))
//                ;
//        // @formatter:on
        //
        // return client;
    }

    /**
     * @param ports int[]
     * @return {@link List}
     * @throws Exception Falls was schief geht.
     */
    private static List<Disposable> startServers(final int...ports) throws Exception
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
        SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

        // @formatter:off
        List<Disposable> servers = IntStream.of(ports)
                .mapToObj(port ->
                    RSocketServer
                        .create()
                        .acceptor(SocketAcceptor.forRequestResponse(payload -> {
                                    LOGGER.info("Server {} got {}", port, payload.getDataUtf8());
                                    return Mono.just(DefaultPayload.create("Server " + port + " response")).delayElement(Duration.ofMillis(100));
                                  })
                         )
                        //.payloadDecoder(PayloadDecoder.ZERO_COPY)
                        .resume(resume)
                        //.resume()
                        //.resumeSessionDuration(Duration.ofMinutes(5))
                        .bind(TcpServerTransport.create(TcpServer.create()
                                .host("localhost")
                                .port(port)
                                .runOn(LoopResources.create("server-" + port, 1, 2, true))
                                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContextBuilder))
                                )
                        )
                        //.bind(LocalServerTransport.create("test-local-" + port))
                        //.subscribe()
                        .block()
                )
                .collect(Collectors.toList())
                ;
        // @formatter:on

        return servers;
    }
}
