// Created: 24.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.fileHandle.FileHandle;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverRSocket extends AbstractReceiver
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
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link RemoteReceiverRSocket} Object.
     */
    public RemoteReceiverRSocket()
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
        this.client = RSocketConnector.create()
              .payloadDecoder(PayloadDecoder.DEFAULT)
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
            .block() // Wartet auf jeden Response.
            //.subscribe() // Führt alles im Hintergrund aus.
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_CREATE_DIRECTORY);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativePath);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_DELETE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativePath);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
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
            .block()
            ;
        // @formatter:on

        this.client.block().dispose();
        this.client = null;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            // Die weitere Verarbeitung soll in einem separaten Thread erfolgen.
            // Da der Consumer die Methode getChecksum aufruft, befindet sich dieser im gleichen reactivem-Thread.
            // Dort sind die #block-Methoden verboten -> "block()/blockFirst()/blockLast() are blocking, which is not supported in thread ..."
            //
            // Siehe JavaDoc von Schedulers
            // #elastic(): Optimized for longer executions, an alternative for blocking tasks where the number of active tasks (and threads) can grow indefinitely
            // #boundedElastic(): Optimized for longer executions, an alternative for blocking tasks where the number of active tasks (and threads) is capped
            .publishOn(Schedulers.elastic())
            .map(payload -> {
                ByteBuf byteBuf = payload.data();

                int itemCount = getSerializer().readFrom(byteBuf, int.class);

                for (int i = 0; i < itemCount; i++)
                {
                    SyncItem syncItem = getSerializer().readFrom(byteBuf, SyncItem.class);

                    consumerSyncItem.accept(syncItem);
                }

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
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_CHECKSUM);

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

    // /**
    // * @see de.freese.jsync.filesystem.receiver.Receiver#getResource(java.lang.String, java.lang.String, long)
    // */
    // @Override
    // public WritableResource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    // {
    // ByteBuf byteBufMeta = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_WRITEABLE_RESOURCE);
    //
    // ByteBuf byteBufData = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufData, baseDir);
    // getSerializer().writeTo(byteBufData, relativeFile);
    // getSerializer().writeTo(byteBufData, sizeOfFile);
    //
//        // @formatter:off
////        this.client.block()
////            .requestChannel(payloads)
////            .map(payload -> {
////                String data = payload.getDataUtf8();
////                payload.release();
////                return data;
////            })
////            .doOnNext(getLogger()::debug)
////            .doOnError(th -> getLogger().error(null, th))
////            .block()
////            ;
//        // @formatter:on
    //
    // return null;
    // }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    private Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_UPDATE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, syncItem);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            //.map(payload -> {
            //    String data = payload.getDataUtf8();
            //    payload.release();
            //    return data;
            //})
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_VALIDATE_FILE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, syncItem);
        getSerializer().writeTo(byteBufData, withChecksum);

        // @formatter:off
        this.client.block()
            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
            //.map(payload -> {
            //    String data = payload.getDataUtf8();
            //    payload.release();
            //    return data;
            //})
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    // /**
    // * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
    // */
    // @Override
    // public void writeChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    // {
    // ByteBuf byteBufMeta = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_WRITE_CHUNK);
    //
    // ByteBuf byteBufData = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufData, baseDir);
    // getSerializer().writeTo(byteBufData, relativeFile);
    // getSerializer().writeTo(byteBufData, position);
    // getSerializer().writeTo(byteBufData, sizeOfChunk);
    //
    // byteBufData.writeBytes(byteBuffer);
    //
//        // @formatter:off
//        this.client.block()
//            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
//            //.map(payload -> {
//            //    String data = payload.getDataUtf8();
//            //    payload.release();
//            //    return data;
//            //})
//            .map(Payload::getDataUtf8)
//            .doOnNext(getLogger()::debug)
//            .doOnError(th -> getLogger().error(null, th))
//            .block()
//            ;
//        // @formatter:on
    // }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeFileHandle(java.lang.String, java.lang.String, long,
     *      de.freese.jsync.filesystem.fileHandle.FileHandle, java.util.function.LongConsumer)
     */
    @Override
    public void writeFileHandle(final String baseDir, final String relativeFile, final long sizeOfFile, final FileHandle fileHandle,
                                final LongConsumer bytesWrittenConsumer)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_WRITE_FILE_HANDLE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);
        getSerializer().writeTo(byteBufData, sizeOfFile);

        Flux<ByteBuffer> fluxByteBuffer = fileHandle.getHandle();

        Flux<Payload> flux = Flux.concat(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)), fluxByteBuffer.map(ByteBufPayload::create));

        // @formatter:off
        this.client.block()
          .requestChannel(flux)
          .map(Payload::getDataUtf8)
          .doOnNext(getLogger()::debug)
          .doOnError(th -> getLogger().error(null, th))
          .then()
          .block()
          ;
      // @formatter:on
    }
}
