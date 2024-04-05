// Created: 18.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;

import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public abstract class AbstractRSocketFileSystem extends AbstractFileSystem {
    private static final ByteBufferPool BYTEBUFFER_POOL = ByteBufferPool.DEFAULT;

    protected static ByteBufferPool getByteBufferPool() {
        return BYTEBUFFER_POOL;
    }

    private final Serializer<ByteBuffer, ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    private RSocketClient client;

    @Override
    public void disconnect() {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.DISCONNECT);

        getClient()
                .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, bufferMeta.flip()))
                        .doOnSubscribe(subscription -> getByteBufferPool().free(bufferMeta))
                )
                .map(Payload::getDataUtf8)
                .doOnNext(getLogger()::debug)
                .doOnError(th -> getLogger().warn(th.getMessage()))
                .block()
        ;

        getClient().dispose();
        this.client = null;

        Schedulers.shutdownNow();
    }

    protected void connect(final URI uri, final UnaryOperator<TcpClient> tcpClientCustomizer) {
        if ("rsocket".equals(uri.getScheme())) {
            this.client = createClientRemote(uri, tcpClientCustomizer);
        }
        else {
            this.client = createClientLocal(uri, tcpClientCustomizer);
        }

        final ByteBuffer bufferMeta = getByteBufferPool().get();

        getSerializer().writeTo(bufferMeta, JSyncCommand.CONNECT);

        this.client
                .requestResponse(Mono.just(DefaultPayload.create(DefaultPayload.EMPTY_BUFFER, bufferMeta.flip()))
                        .doOnSubscribe(subscription -> getByteBufferPool().free(bufferMeta))
                )
                .map(Payload::getDataUtf8)
                .doOnNext(getLogger()::debug)
                .doOnError(th -> getLogger().error(th.getMessage(), th))
                .block()
        //.subscribe()
        ;
    }

    protected RSocketClient createClientLocal(final URI uri, final UnaryOperator<TcpClient> tcpClientCustomizer) {
        return RSocketBuilders.clientLocal()
                .name("jSync")
                .logger(getLogger())
                .build()
                ;
    }

    protected RSocketClient createClientRemote(final URI uri, final UnaryOperator<TcpClient> tcpClientCustomizer) {
        return RSocketBuilders.clientRemote()
                .remoteAddress(new InetSocketAddress(uri.getHost(), uri.getPort()))
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(getLogger())
                .addTcpClientCustomizer(tcpClientCustomizer)
                .build()
                ;
    }

    protected String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead, final JSyncCommand command) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, command);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativeFile);

        return getClient()
                .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                        .doOnSubscribe(subscription -> {
                            getByteBufferPool().free(bufferMeta);
                            getByteBufferPool().free(bufferData);
                        })
                )
                .map(Payload::getDataUtf8)
                .doOnNext(getLogger()::debug)
                .doOnError(th -> getLogger().error(th.getMessage(), th))
                .doOnNext(value -> {
                    if (PATTERN_NUMBER.matcher(value).matches()) {
                        consumerChecksumBytesRead.accept(Long.parseLong(value));
                    }
                })
                .blockLast()
                ;
    }

    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter, final JSyncCommand command) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, command);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, followSymLinks);
        getSerializer().writeTo(bufferData, pathFilter);

        return getClient()
                .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                        .doOnSubscribe(subscription -> {
                            getByteBufferPool().free(bufferMeta);
                            getByteBufferPool().free(bufferData);
                        })
                )
                .publishOn(Schedulers.boundedElastic()) // Consumer calls generateChecksum = swap to another Thread or an Exception is caused !
                .doOnError(th -> getLogger().error(th.getMessage(), th))
                .map(payload -> {
                    final ByteBuffer buffer = payload.getData();
                    return getSerializer().readFrom(buffer, SyncItem.class);
                })
                ;
    }

    protected RSocketClient getClient() {
        return this.client;
    }

    protected Serializer<ByteBuffer, ByteBuffer> getSerializer() {
        return this.serializer;
    }
}
