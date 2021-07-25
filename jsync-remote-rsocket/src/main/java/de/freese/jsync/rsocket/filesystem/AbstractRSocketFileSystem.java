// Created: 18.07.2021
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.utils.pool.ByteBufferPool;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
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
        TcpClient tcpClient = TcpClient.create()
                .host(uri.getHost())
                .port(uri.getPort())
                .doOnConnected(connection -> getLogger().info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> getLogger().info("Disconnected: {}", connection.channel()))
                ;
        // @formatter:on

        tcpClient = tcpClientCustomizer.apply(tcpClient);

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
        // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(s -> getLogger().info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        RSocketConnector connector = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                // .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                .resume(resume)
                ;
        // @formatter:on

        Mono<RSocket> rSocket = connector.connect(clientTransport);

        this.client = RSocketClient.from(rSocket);

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
     * @param relativeFile relativeFile
     * @param checksumBytesReadConsumer checksumBytesReadConsumer
     * @param command {@link JSyncCommand}
     *
     * @return String
     */
    protected String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer checksumBytesReadConsumer, final JSyncCommand command)
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
     * @param consumerSyncItem {@link Consumer}
     * @param command {@link JSyncCommand}
     */
    protected void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem, final JSyncCommand command)
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
                consumerSyncItem.accept(syncItem);
            })
            .blockLast()
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
