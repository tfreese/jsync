// Created: 22.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.springframework.core.io.Resource;
import de.freese.jsync.filesystem.sender.AbstractSender;
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
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RemoteSenderRSocket extends AbstractSender
{
    /**
    *
    */
    private final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    /**
     *
     */
    private Mono<RSocket> client;

    /**
     * {@link Executors#newCachedThreadPool()}
     */
    private final Executor executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

    /**
     *
     */
    private final Scheduler jsyncScheduler = Schedulers.fromExecutor(this.executor);

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link RemoteSenderRSocket} Object.
     */
    public RemoteSenderRSocket()
    {
        super();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // @formatter:off
        this.client = RSocketConnector
              .create()
              .payloadDecoder(PayloadDecoder.ZERO_COPY)
              .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
              //.reconnect(Retry.backoff(50, Duration.ofMillis(500)))
              .connect(TcpClientTransport.create(TcpClient.create()
                  .host(uri.getHost())
                  .port(uri.getPort())
                  .runOn(LoopResources.create("jsync-client-sender-", 3, true))
                  )
              )
              //.connect(LocalClientTransport.create("test-local-" + port))
              ;
        // @formatter:on

        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.CONNECT);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            //.doFinally(signalType -> byteBufMeta.release())
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
        this.client.block()
            .requestResponse(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            //.doFinally(signalType -> byteBufMeta.release())
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            // Die weitere Verarbeitung soll in einem separaten Thread erfolgen.
            // Da der Consumer die Methode getChecksum aufruft, befindet sich dieser im gleichen reactivem-Thread.
            // Dort sind die #block-Methoden verboten -> "block()/blockFirst()/blockLast() are blocking, which is not supported in thread ..."
            .publishOn(this.jsyncScheduler)
            .map(payload -> {
                ByteBuf byteBuf = payload.data();

                int itemCount = getSerializer().readFrom(byteBuf, int.class);

                for (int i = 0; i < itemCount; i++)
                {
                    SyncItem syncItem = getSerializer().readFrom(byteBuf, SyncItem.class);

                    consumerSyncItem.accept(syncItem);
                }

                byteBuf.release();

                return Mono.empty();
            })
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @return {@link ByteBufAllocator}
     */
    private ByteBufAllocator getByteBufAllocator()
    {
        return this.byteBufAllocator;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_CHECKSUM);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);

        // @formatter:off
        String checksum = this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getResource(java.lang.String, java.lang.String, long)
     */
    @Override
    public Resource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    private Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    {
        // TODO Auto-generated method stub

    }
}
