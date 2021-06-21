// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author Thomas Freese
 */
public final class RSocketLoadBalancerDemo
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketLoadBalancerDemo.class);

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht
     */
    public static void main(final String[] args) throws Exception
    {
        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("rsocket"));
        // TcpResources.set(ConnectionProvider.create("rsocket-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th -> {
        });

        // SocketUtils.findAvailableTcpPort()
        List<SocketAddress> serverAddresses = Stream.of(7000, 8000, 9000).map(port -> new InetSocketAddress("localhost", port)).collect(Collectors.toList());

        List<Disposable> servers = Server.startServer(serverAddresses);
        // RSocketClient client = Client.createClient(serverAddresses.get(0));
        // RSocketClient client = Client.createClientLoadBalanced(serverAddresses);
        RSocketClient client = Client.createClientLoadBalancedWithServiceDiscovery(serverAddresses);

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        Flux.range(0, 30).parallel().runOn(Schedulers.boundedElastic())
            .map(i ->
                client
                    .requestResponse(Mono.just(DefaultPayload.create("flux-" + i.intValue())))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                    )
            .subscribe(Mono::subscribe)
            ;
        // @formatter:on

        // Den letzten Server stoppen.
        servers.remove(2).dispose();

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        IntStream.range(0, 30).parallel()
            .mapToObj(i ->
                 client
                    .requestResponse(Mono.just(DefaultPayload.create("intstream-" + i)))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .forEach(Mono::subscribe)
            ;
        // @formatter:on

        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 30; i++)
        {
            TimeUnit.MILLISECONDS.sleep(100);

            // @formatter:off
            client
                .requestResponse(Mono.just(DefaultPayload.create("for-" + i)))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                //.block() // Wartet auf den Response, hier knallt es bei ungültigen Connections (ServiceDiscovery).
                .subscribe() // Führt alles im Hintergrund aus.
                ;
            // @formatter:on
        }

        TimeUnit.SECONDS.sleep(2);

        client.dispose();
        servers.forEach(Disposable::dispose);
    }

    /**
     * Erzeugt ein neues {@link RSocketLoadBalancerDemo} Objekt.
     */
    private RSocketLoadBalancerDemo()
    {
        super();
    }
}
