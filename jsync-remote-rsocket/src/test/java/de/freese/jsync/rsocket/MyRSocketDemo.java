// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
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
     */
    public static void main(final String[] args)
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(
                        Retry
                            .fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                            .doBeforeRetry(s -> LOGGER.debug("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // TcpResources.set(LoopResources.create("demo-threadPool", 2, 4, true));
        // TcpResources.set(ConnectionProvider.create("demo-connectionPool", 16));

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
               .host("localhost")
                .port(7000)
                //.runOn(LoopResources.create("demo-server", 2, 4, true))
                ;
        // @formatter:on

        // @formatter:off
        Disposable server = RSocketServer
                .create(SocketAcceptor.with(new MyRSocketHandler()))
                .resume(resume)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                //.bind(TcpServerTransport.create("localhost", 7000))
                .bind(TcpServerTransport.create(tcpServer))
                //.subscribe()
                .block()
        ;
        // @formatter:on

        // Mono<RSocket> source =
        // RSocketConnector.create()
        // .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
        // .connect(TcpClientTransport.create("localhost", 7000));
        //
        // RSocketClient.from(source)

        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                .host("localhost")
                .port(7000)
               //.runOn(LoopResources.create("demo-client", 4, true))
                ;
        // @formatter:on

        // @formatter:off
        RSocket socket = RSocketConnector
                .create()
                //.resume(resume)
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                //.connectWith(TcpClientTransport.create("localhost", 7000))
                .connect(TcpClientTransport.create(tcpClient))
               //.connectWith(TcpClientTransport.create(tcpClient))
                .block()
                ;
        // @formatter:on

        // Request - Response
        for (int i = 0; i < 3; i++)
        {
            // @formatter:off
            socket
                .requestResponse(DefaultPayload.create("Hello", "meta"))
                .map(pl -> {
                    String response = pl.getDataUtf8();
                    pl.release();

                    return response;
                })
                .doOnError(th -> LOGGER.error(th.getMessage()))
                .onErrorReturn("error")
                .doOnNext(LOGGER::debug)
                .block()
                ;
            // @formatter:on
        }

        // Channel: Erwartung = '/tmp/test.txt' mit 'Hello World !' wird geschrieben.
        Payload first = DefaultPayload.create("Hello", "/tmp/test.txt");
        Payload second = DefaultPayload.create(" World !");

        // @formatter:off
        socket
            .requestChannel(Flux.just(first, second))
            .map(Payload::getDataUtf8)
            .doOnNext(LOGGER::debug)
            .then()
            .block()
            ;
        // @formatter:on

        server.dispose();
        socket.dispose();
    }
}