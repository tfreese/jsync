// Created: 09.07.2021
package de.freese.jsync.http;

import java.util.function.Consumer;

import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.HttpResources;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;
import reactor.util.function.Tuple2;

/**
 * @author Thomas Freese
 */
public class ReactiveHttp
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveHttp.class);

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schiefgeht.
     */
    public static void main(final String[] args) throws Exception
    {
        HttpResources.set(LoopResources.create("reactive-http", 2, 4, true));

        // @formatter:off
        DisposableServer httpServer = HttpServer.create()
            .protocol(HttpProtocol.H2C)
            .route(routes -> routes
                    .get("/hello", (request, response) -> response.sendString(Mono.just("Hello World!")))
                    .post("/echo", (request, response) -> response.send(request.receive().retain()))
                    .get("/path/{param}", (request, response) -> response.sendString(Mono.just(request.param("param"))))
                    .ws("/ws", (wsInbound, wsOutbound) -> wsOutbound.send(wsInbound.receive().retain()))
            )
            .doOnBound(server -> LOGGER.info("Server started on port: {}", server.port()))
            .doOnUnbound(server -> LOGGER.info("Server stopped on port: {}", server.port()))
            //.runOn(new NioEventLoopGroup(3, new JSyncThreadFactory("server-"))) // EpollEventLoopGroup geht nur auf Linux
            .bindNow()
            ;
        // @formatter:on

        // @formatter:off
        HttpClient httpClient = HttpClient.create()
            .host("localhost")
            .port(httpServer.port())
            .protocol(HttpProtocol.H2C)
            //.runOn(new NioEventLoopGroup(3, new JSyncThreadFactory("client-"))) // EpollEventLoopGroup geht nur auf Linux
            ;
        // @formatter:on

        httpClient.get().uri("/hello").responseContent().aggregate().asString().subscribe(LOGGER::info);
        httpClient.post().uri("/echo").send(ByteBufFlux.fromString(Mono.just("Echo!"))).responseContent().aggregate().asString().subscribe(LOGGER::info);

        Consumer<Tuple2<String, HttpHeaders>> responseSubscriber = response ->
        {
            LOGGER.info("Response: {}", response.getT1());
            LOGGER.info("Used stream ID: {}", response.getT2().get("x-http2-stream-id"));
        };

        for (int i = 0; i < 3; i++)
        {
            Tuple2<String, HttpHeaders> response =
                    httpClient.get().uri("/hello").responseSingle((res, bytes) -> bytes.asString().zipWith(Mono.just(res.responseHeaders()))).block();

            // Ausgabe im main-Thread.
            responseSubscriber.accept(response);

            httpClient.get().uri("/hello").responseSingle((res, bytes) -> bytes.asString().zipWith(Mono.just(res.responseHeaders())))
                    .subscribe(responseSubscriber);
        }

        // httpServer.onDispose().block(); // Wartet
        httpServer.dispose();
    }
}
