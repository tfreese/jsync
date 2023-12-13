// Created: 20.07.2021
package de.freese.jsync.tcp;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpResources;
import reactor.netty.tcp.TcpServer;

/**
 * @author Thomas Freese
 */
public final class ReactiveTcp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveTcp.class);

    public static void main(final String[] args) throws Exception {
        TcpResources.set(LoopResources.create("tcpServer", 2, 4, true));

        // @formatter:off
        final DisposableServer tcpServer = TcpServer.create()
                .handle((in, out) -> {
                    final Flux<String> inFlux = in.receive()
                            .asString(StandardCharsets.UTF_8)
                            .doOnNext(request -> LOGGER.info("Server: Request = {}", request))
                            ;

                    //return out.sendString(Flux.concat(inFlux, Mono.just(" World !")), StandardCharsets.UTF_8);
                    //return out.sendString(inFlux.concatWith(Mono.just(" World !")), StandardCharsets.UTF_8);
                    return out.sendString(inFlux.concatWithValues(" World !"), StandardCharsets.UTF_8);
                })
                .doOnBound(server -> LOGGER.info("Server started on port: {}", server.port()))
                .bindNow()
                ;
        // @formatter:on

        // @formatter:off
        final Connection connection = TcpClient.create()
                .port(tcpServer.port())
                .connectNow()
                ;
        // @formatter:on

        // .aggregate()
        // .collect(Collectors.joining())
        connection.outbound().sendString(Mono.just("Hello"), StandardCharsets.UTF_8).then().subscribe();

        // Complete Response is missing, only the Request 'Hello' is displayed ?!
        connection.inbound().receive().asString(StandardCharsets.UTF_8).subscribe(response -> LOGGER.info("Client: Response = {}", response));

        TimeUnit.SECONDS.sleep(1);

        // connection.onDispose().block();
        // tcpServer.onDispose().block();

        connection.dispose();
        tcpServer.dispose();
    }

    private ReactiveTcp() {
        super();
    }
}
