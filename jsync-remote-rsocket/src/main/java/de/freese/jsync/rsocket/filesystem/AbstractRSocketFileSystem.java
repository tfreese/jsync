// Created: 18.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRSocketFileSystem extends AbstractFileSystem
{
    /**
    *
    */
    private final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    /**
    *
    */
    private RSocketClient client;

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * @param uri {@link URI}
     * @param tcpClientCustomizer {@link Function}
     */
    protected void connect(final URI uri, final Function<TcpClient, TcpClient> tcpClientCustomizer)
    {
        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                .host(uri.getHost())
                .port(uri.getPort())
                ;
        // @formatter:on

        tcpClient = tcpClientCustomizer.apply(tcpClient);

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
        // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

        // @formatter:off
        RSocketConnector connector = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                // .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                ;
        // @formatter:on

        Mono<RSocket> rSocket = connector.connect(clientTransport);

        this.client = RSocketClient.from(rSocket);

        // Connect an den Server schicken.
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.CONNECT);

        // @formatter:off
        this.client
            .requestResponse(Mono.just(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta)))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block() // Wartet auf jeden Response.
            //.subscribe() // Führt alles im Hintergrund aus.
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.DISCONNECT);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta)))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on

        getClient().dispose();
        this.client = null;
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param withChecksum boolean
     * @param consumerBytesRead {@link LongConsumer}
     * @param command {@link JSyncCommand}
     *
     * @return {@link Flux}
     */
    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final boolean withChecksum,
                                               final LongConsumer consumerBytesRead, final JSyncCommand command)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, command);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, followSymLinks);
        getSerializer().writeTo(byteBufData, withChecksum);

        // Sinks.Many<SyncItem> latestChange = Sinks.many().replay().all();
        // latestChange.asFlux().publishOn(Schedulers.boundedElastic()).subscribe(LOGGER::info);
        // latestChange.tryEmitNext("--");

        // @formatter:off
        return getClient()
            .requestResponse(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)))
            // Die weitere Verarbeitung soll in einem separaten Thread erfolgen.
            // Da der Consumer die Methode getChecksum aufruft, befindet sich dieser im gleichen reactivem-Thread.
            // Dort sind die #block-Methoden verboten -> "block()/blockFirst()/blockLast() are blocking, which is not supported in thread ..."
            //
            // Siehe JavaDoc von Schedulers
            // #boundedElastic(): Optimized for longer executions, an alternative for blocking tasks where the number of active tasks (and threads) is capped
            .publishOn(Schedulers.boundedElastic())
            .flatMapMany(payload -> {
                ByteBuf byteBuf = payload.data();

                int itemCount = getSerializer().readFrom(byteBuf, int.class);
                List<SyncItem> syncItems = new ArrayList<>();

                for (int i = 0; i < itemCount; i++)
                {
                    SyncItem syncItem = getSerializer().readFrom(byteBuf, SyncItem.class);

                    syncItems.add(syncItem);
                }

                //payload.release();
                //byteBuf.release();

                return Flux.fromIterable(syncItems);
            })
            .doOnError(th -> getLogger().error(null, th))
            ;
        // @formatter:on
    }

    /**
     * @return {@link ByteBufAllocator}
     */
    protected ByteBufAllocator getByteBufAllocator()
    {
        return this.byteBufAllocator;
    }

    /**
     * @return {@link RSocketClient}
     */
    protected RSocketClient getClient()
    {
        return this.client;
    }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    protected Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

}
