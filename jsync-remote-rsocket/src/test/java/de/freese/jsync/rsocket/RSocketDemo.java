// Created: 11.07.2021
package de.freese.jsync.rsocket;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.rsocket.client.MyRSocketClient;
import de.freese.jsync.rsocket.client.MyRSocketClientLocal;
import de.freese.jsync.rsocket.client.MyRSocketClientRemote;
import de.freese.jsync.rsocket.client.MyRSocketClientRemoteLoadBalanced;
import de.freese.jsync.rsocket.client.MyRSocketClientRemoteLoadBalancedWithServiceDiscovery;
import de.freese.jsync.rsocket.server.MyRSocketServer;
import de.freese.jsync.rsocket.server.MyRSocketServerLocal;
import de.freese.jsync.rsocket.server.MyRSocketServerRemote;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * @author Thomas Freese
 */
public class RSocketDemo
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketDemo.class);

    /**
     * Remote
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> createRemote(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception
    {
        MyRSocketServerRemote myServer = new MyRSocketServerRemote("localhost", 6000, socketAcceptor);
        myServer.start();

        MyRSocketClientRemote myClient = new MyRSocketClientRemote();
        myClient.connect(myServer.getSocketAddress());

        return Tuples.of(myClient, List.of(myServer));
    }

    /**
     * Remote mit Client-LoadBalancer.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> createRemoteWithLoadBalancer(final Function<Integer, SocketAcceptor> socketAcceptor)
        throws Exception
    {
        MyRSocketServerRemote myServer1 = new MyRSocketServerRemote("localhost", 6000, socketAcceptor);
        MyRSocketServerRemote myServer2 = new MyRSocketServerRemote("localhost", 7000, socketAcceptor);

        myServer1.start();
        myServer2.start();

        MyRSocketClientRemoteLoadBalanced myClient = new MyRSocketClientRemoteLoadBalanced();
        myClient.connect(List.of(myServer1.getSocketAddress(), myServer2.getSocketAddress()));

        return Tuples.of(myClient, List.of(myServer1, myServer2));
    }

    /**
     * Remote mit Client-LoadBalancer und Service-Discovery.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> createRemoteWithLoadBalancerAndServiceDiscovery(final Function<Integer, SocketAcceptor> socketAcceptor)
        throws Exception
    {
        MyRSocketServerRemote myServer1 = new MyRSocketServerRemote("localhost", 6000, socketAcceptor);
        MyRSocketServerRemote myServer2 = new MyRSocketServerRemote("localhost", 7000, socketAcceptor);
        MyRSocketServerRemote myServer3 = new MyRSocketServerRemote("localhost", 8000, socketAcceptor);
        MyRSocketServerRemote myServer4 = new MyRSocketServerRemote("localhost", 9000, socketAcceptor);

        myServer1.start();
        myServer2.start();
        myServer3.start();
        myServer4.start();

        MyRSocketClientRemoteLoadBalancedWithServiceDiscovery myClient = new MyRSocketClientRemoteLoadBalancedWithServiceDiscovery();
        myClient.connect(List.of(myServer1.getSocketAddress(), myServer2.getSocketAddress(), myServer3.getSocketAddress(), myServer4.getSocketAddress()));

        return Tuples.of(myClient, List.of(myServer1, myServer2, myServer3, myServer4));
    }

    /**
     * Client und Server in der gleichen Runtime ohne Network-Stack.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> createSameVm(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception
    {
        MyRSocketServerLocal myServer = new MyRSocketServerLocal("test1", socketAcceptor);
        myServer.start();

        MyRSocketClient<MyRSocketServerLocal> myClient = new MyRSocketClientLocal();
        myClient.connect(myServer);

        return Tuples.of(myClient, List.of(myServer));
    }

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        // Globale Default-Resourcen.
        TcpResources.set(LoopResources.create("rsocket"));
        // TcpResources.set(LoopResources.create("rsocket", 2, 8, true));
        // TcpResources.set(ConnectionProvider.create("rsocket-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th -> {
        });

        // Debug einschalten.
        Hooks.onOperatorDebug();

        Function<Integer, SocketAcceptor> socketAcceptor = port -> SocketAcceptor.forRequestResponse(payload -> {
            String request = payload.getDataUtf8();
            LOGGER.info("Server {} got request {}", port, request);
            return Mono.just(DefaultPayload.create("Client of Server " + port + " got response " + request));
        });

        // Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> tuple = createSameVm(socketAcceptor);
        // Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> tuple = createRemote(socketAcceptor);
        // Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> tuple = createRemoteWithLoadBalancer(socketAcceptor)
        Tuple2<MyRSocketClient<?>, List<MyRSocketServer>> tuple = createRemoteWithLoadBalancerAndServiceDiscovery(socketAcceptor);

        MyRSocketClient<?> myRSocketClient = tuple.getT1();
        List<MyRSocketServer> servers = tuple.getT2();

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        Flux.range(0, 30).parallel().runOn(Schedulers.boundedElastic())
            .map(i ->
                myRSocketClient.getClient()
                    .requestResponse(Mono.just(DefaultPayload.create("flux-" + i.intValue())))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .subscribe(Mono::subscribe)
            ;
        // @formatter:on

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        IntStream.range(0, 30).parallel()
            .mapToObj(i ->
                myRSocketClient.getClient()
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
            myRSocketClient.getClient()
                .requestResponse(Mono.just(DefaultPayload.create("for-" + i)))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                //.block() // Wartet auf den Response, hier knallt es bei ungültigen Connections (ServiceDiscovery).
                .subscribe() // Führt alles im Hintergrund aus.
                ;
            // @formatter:on
        }

        TimeUnit.SECONDS.sleep(2);
        // System.in.read();

        myRSocketClient.disconnect();
        servers.forEach(MyRSocketServer::stop);
    }
}
