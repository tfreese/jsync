// Created: 09.07.2021
package de.freese.jsync.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
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
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
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
            .bindNow()
            ;
        // @formatter:on

        // @formatter:off
        HttpClient httpClient = HttpClient.create()
            .host("localhost")
            .port(httpServer.port())
            .protocol(HttpProtocol.H2C)
            ;
          // @formatter:on

        httpClient.get().uri("/hello").responseContent().aggregate().asString().subscribe(System.out::println);
        httpClient.post().uri("/echo").send(ByteBufFlux.fromString(Mono.just("Echo!"))).responseContent().aggregate().asString().subscribe(System.out::println);

        for (int i = 0; i < 3; i++)
        {
            Tuple2<String, HttpHeaders> response =
                    httpClient.get().uri("/hello").responseSingle((res, bytes) -> bytes.asString().zipWith(Mono.just(res.responseHeaders()))).block();
            System.out.println();
            System.out.println("Response: " + response.getT1());
            System.out.println("Used stream ID: " + response.getT2().get("x-http2-stream-id"));
        }

        httpServer.onDispose().block();
    }
}
