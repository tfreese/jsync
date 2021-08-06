// Created: 18.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.utils.pool.ByteBufferPool;
import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRSocketFileSystem extends AbstractFileSystem
{
    /**
     *
     */
    private final ByteBufferPool byteBufferPool = ByteBufferPool.getInstance();

    /**
    *
    */
    private RSocketClient client;

    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * @param uri {@link URI}
     * @param tcpClientCustomizer {@link Function}
     */
    protected void connect(final URI uri, final Function<TcpClient, TcpClient> tcpClientCustomizer)
    {
        // @formatter:off
        this.client = RSocketBuilders.clientRemote()
                .remoteAddress(new InetSocketAddress(uri.getHost(), uri.getPort()))
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(getLogger())
                .addTcpClientCustomizer(tcpClientCustomizer)
                .build()
                ;
        // @formatter:on

        // Connect an den Server schicken.
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();

        getSerializer().writeTo(byteBufMeta, JSyncCommand.CONNECT);

        // @formatter:off
        this.client
            .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, byteBufMeta.flip())).doOnSubscribe(subscription -> getByteBufferPool().free(byteBufMeta)))
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
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.DISCONNECT);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, byteBufMeta.flip())).doOnSubscribe(subscription -> getByteBufferPool().free(byteBufMeta)))
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
     * @param relativeFile String
     * @param consumerChecksumBytesRead consumerChecksumBytesRead
     * @param command {@link JSyncCommand}
     *
     * @return String
     */
    protected String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead, final JSyncCommand command)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, command);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);

        // @formatter:off
        return getClient()
                .requestResponse(Mono.just(DefaultPayload.create(byteBufData.flip(), byteBufMeta.flip())).doOnSubscribe(subscription -> {
                    getByteBufferPool().free(byteBufMeta);
                    getByteBufferPool().free(byteBufData);
                    })
                )
                .map(Payload::getDataUtf8)
                .doOnNext(getLogger()::debug)
                .doOnError(th -> getLogger().error(null, th))
                .block()
                ;
        // @formatter:on
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumer {@link Consumer}
     * @param command {@link JSyncCommand}
     */
    protected void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumer, final JSyncCommand command)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, command);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        getClient()
            .requestStream(Mono.just(DefaultPayload.create(byteBufData.flip(), byteBufMeta.flip())).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
                })
            )
            .publishOn(Schedulers.boundedElastic()) // Consumer ruft generateChecksum auf -> in anderen Thread auslagern sonst knallts !
            .doOnError(th -> getLogger().error(null, th))
            .doOnNext(payload -> {
                ByteBuffer byteBuf = payload.getData();
                SyncItem syncItem = getSerializer().readFrom(byteBuf, SyncItem.class);
                consumer.accept(syncItem);
            })
            .blockLast()
            ;
        // @formatter:on
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param command {@link JSyncCommand}
     *
     * @return {@link Flux}
     */
    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final JSyncCommand command)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, command);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        return getClient()
            .requestStream(Mono.just(DefaultPayload.create(byteBufData.flip(), byteBufMeta.flip())).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
                })
            )
            .publishOn(Schedulers.boundedElastic()) // Consumer ruft generateChecksum auf -> in anderen Thread auslagern sonst knallts !
            .doOnError(th -> getLogger().error(null, th))
            .map(payload -> {
                ByteBuffer byteBuf = payload.getData();
                return getSerializer().readFrom(byteBuf, SyncItem.class);
            })
            ;
        // @formatter:on
    }

    /**
     * @return {@link ByteBufferPool}
     */
    protected ByteBufferPool getByteBufferPool()
    {
        return this.byteBufferPool;
    }

    /**
     * @return {@link RSocketClient}
     */
    protected RSocketClient getClient()
    {
        return this.client;
    }

    /**
     * @return {@link Serializer}
     */
    protected Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }
}
