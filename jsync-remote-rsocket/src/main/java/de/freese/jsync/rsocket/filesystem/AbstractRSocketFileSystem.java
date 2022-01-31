// Created: 18.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterTrue;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
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
    private final ByteBufferPool byteBufferPool = ByteBufferPool.DEFAULT;
    /**
    *
    */
    private RSocketClient client;
    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.DISCONNECT);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, bufferMeta.flip()))
                    .doOnSubscribe(subscription -> getByteBufferPool().free(bufferMeta))
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().warn(th.getMessage()))
            .block()
            ;
        // @formatter:on

        getClient().dispose();
        this.client = null;

        Schedulers.shutdownNow();
    }

    /**
     * @param uri {@link URI}
     * @param tcpClientCustomizer {@link Function}
     */
    protected void connect(final URI uri, final Function<TcpClient, TcpClient> tcpClientCustomizer)
    {
        if ("rsocket".equals(uri.getScheme()))
        {
            this.client = createClientRemote(uri, tcpClientCustomizer);
        }
        else
        {
            this.client = createClientLocal(uri, tcpClientCustomizer);
        }

        // Connect an den Server schicken.
        ByteBuffer bufferMeta = getByteBufferPool().get();

        getSerializer().writeTo(bufferMeta, JSyncCommand.CONNECT);

        // @formatter:off
        this.client
            .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, bufferMeta.flip()))
                    .doOnSubscribe(subscription -> getByteBufferPool().free(bufferMeta))
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block() // Wartet auf jeden Response.
            //.subscribe() // Führt alles im Hintergrund aus.
            ;
        // @formatter:on
    }

    /**
     * @param uri {@link URI}
     * @param tcpClientCustomizer {@link Function}
     *
     * @return {@link RSocketClient}
     */
    protected RSocketClient createClientLocal(final URI uri, final Function<TcpClient, TcpClient> tcpClientCustomizer)
    {
        // @formatter:off
        return RSocketBuilders.clientLocal()
                .name("jsync")
                .logger(getLogger())
                .build()
                ;
        // @formatter:on
    }

    /**
     * @param uri {@link URI}
     * @param tcpClientCustomizer {@link Function}
     *
     * @return {@link RSocketClient}
     */
    protected RSocketClient createClientRemote(final URI uri, final Function<TcpClient, TcpClient> tcpClientCustomizer)
    {
        // @formatter:off
        return RSocketBuilders.clientRemote()
                .remoteAddress(new InetSocketAddress(uri.getHost(), uri.getPort()))
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(getLogger())
                .addTcpClientCustomizer(tcpClientCustomizer)
                .build()
                ;
        // @formatter:on
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
        ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, command);

        ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativeFile);

        // @formatter:off
        return getClient()
                .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                        .doOnSubscribe(subscription -> {
                            getByteBufferPool().free(bufferMeta);
                            getByteBufferPool().free(bufferData);
                        })
                )
                .map(Payload::getDataUtf8)
                .doOnNext(getLogger()::debug)
                .doOnError(th -> getLogger().error(null, th))
                .doOnNext(value -> {
                    if(PATTERN_NUMBER.matcher(value).matches())
                    {
                        consumerChecksumBytesRead.accept(Long.parseLong(value));
                    }
                })
                .blockLast()
                ;
        // @formatter:on
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param pathFilter {@link PathFilter}
     * @param command {@link JSyncCommand}
     *
     * @return {@link Flux}
     */
    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter, final JSyncCommand command)
    {
        ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, command);

        ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, followSymLinks);
        getSerializer().writeTo(bufferData, pathFilter != null ? pathFilter : new PathFilterTrue());

        // @formatter:off
        return getClient()
            .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .publishOn(Schedulers.boundedElastic()) // Consumer ruft generateChecksum auf -> in anderen Thread auslagern sonst knallts !
            .doOnError(th -> getLogger().error(null, th))
            .map(payload -> {
                ByteBuffer buffer = payload.getData();
                return getSerializer().readFrom(buffer, SyncItem.class);
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
