// Created: 22.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.fileHandle.FileHandle;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.filesystem.filehandle.FileHandleFluxByteBuffer;
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
    private RSocketClient client;

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                .host(uri.getHost())
                .port(uri.getPort())
                .runOn(LoopResources.create("jsync-client-sender-", 3, true))
                ;
        // @formatter:on

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

        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.CONNECT);

        // @formatter:off
        this.client
            .requestResponse(Mono.just(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta)))
            //.map(payload -> {
            //    String data = payload.getDataUtf8();
            //    payload.release();
            //    return data;
            //})
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
        this.client
            .requestResponse(Mono.just(ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBufMeta)))
            //.map(payload -> {
            //    String data = payload.getDataUtf8();
            //    payload.release();
            //    return data;
            //})
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(null, th))
            //.doFinally(signalType -> byteBufMeta.release())
            .block()
            ;
        // @formatter:on

        this.client.dispose();
        this.client = null;
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
        this.client
            .requestResponse(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)))
            // Die weitere Verarbeitung soll in einem separaten Thread erfolgen.
            // Da der Consumer die Methode getChecksum aufruft, befindet sich dieser im gleichen reactivem-Thread.
            // Dort sind die #block-Methoden verboten -> "block()/blockFirst()/blockLast() are blocking, which is not supported in thread ..."
            //
            // Siehe JavaDoc von Schedulers
            // #boundedElastic(): Optimized for longer executions, an alternative for blocking tasks where the number of active tasks (and threads) is capped
            .publishOn(Schedulers.boundedElastic())
            .map(payload -> {
                ByteBuf byteBuf = payload.data();

                int itemCount = getSerializer().readFrom(byteBuf, int.class);

                for (int i = 0; i < itemCount; i++)
                {
                    SyncItem syncItem = getSerializer().readFrom(byteBuf, SyncItem.class);

                    consumerSyncItem.accept(syncItem);
                }

                //payload.release();
                //byteBuf.release();

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

        return this.client.requestResponse(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)))
                // .map(payload -> {
                // String data = payload.getDataUtf8();
                // payload.release();
                // return data;
                // })
                .map(Payload::getDataUtf8).doOnNext(getLogger()::debug).doOnError(th -> getLogger().error(null, th)).block();
    }

    // /**
    // * @see de.freese.jsync.filesystem.FileSystem#getResource(java.lang.String, java.lang.String, long)
    // */
    // @Override
    // public Resource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    // {
    // ByteBuf byteBufMeta = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_READABLE_RESOURCE);
    //
    // ByteBuf byteBufData = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufData, baseDir);
    // getSerializer().writeTo(byteBufData, relativeFile);
    // getSerializer().writeTo(byteBufData, sizeOfFile);
    //
//        // @formatter:off
//        Flux<DataBuffer> response = this.client.block()
//            .requestStream(ByteBufPayload.create(byteBufData, byteBufMeta))
//            .map(payload -> {
////                System.out.println("RemoteSenderRSocket.getResource()");
//                ByteBuf byteBuf = payload.data();
//                //payload.retain();
//                return ((NettyDataBufferFactory) getDataBufferFactory()).wrap(byteBuf);
//            })
//            .cast(DataBuffer.class)
//            .doOnError(th -> getLogger().error(null, th))
//            ;
//        // @formatter:on
    //
    // InputStream emptyInputStream = new InputStream()
    // {
    // /**
    // * @see java.io.InputStream#read()
    // */
    // @Override
    // public int read()
    // {
    // return -1;
    // }
    // };
    //
//        // @formatter:off
//        InputStream inputStream = response
//            .reduce(emptyInputStream, (in, dataBuffer) -> new SequenceInputStream(in, dataBuffer.asInputStream()))
//            .block()
//            ;
//         // @formatter:on
    //
    // // try
    // // {
    // // PipedOutputStream outPipe = new PipedOutputStream();
    // // PipedInputStream inPipe = new PipedInputStream(outPipe);
    // //
    // // DataBufferUtils.write(response, outPipe).subscribe(DataBufferUtils.releaseConsumer());
    //
    // // RemoteSenderResource senderResource = new RemoteSenderResource(relativeFile, sizeOfFile, Channels.newChannel(inPipe));
    //
    // RemoteSenderResource senderResource = new RemoteSenderResource(relativeFile, sizeOfFile, Channels.newChannel(inputStream));
    //
    // return senderResource;
    // // }
    // // catch (IOException ex)
    // // {
    // // throw new UncheckedIOException(ex);
    // // }
    // }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    private Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

    // /**
    // * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
    // */
    // @Override
    // public void readChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    // {
    // ByteBuf byteBufMeta = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_READ_CHUNK);
    //
    // ByteBuf byteBufData = getByteBufAllocator().buffer();
    // getSerializer().writeTo(byteBufData, baseDir);
    // getSerializer().writeTo(byteBufData, relativeFile);
    // getSerializer().writeTo(byteBufData, position);
    // getSerializer().writeTo(byteBufData, sizeOfChunk);
    //
//        // @formatter:off
//        this.client.block()
//            .requestResponse(ByteBufPayload.create(byteBufData, byteBufMeta))
//            .map(payload -> {
//                ByteBuffer byteBufferData = payload.getData();
//                //payload.release();
//
//                byteBuffer.clear();
//                return byteBuffer.put(byteBufferData);
//            })
//            .doOnError(th -> getLogger().error(null, th))
//            .block()
//            ;
//        // @formatter:on
    // }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readFileHandle(java.lang.String, java.lang.String, long)
     */
    @Override
    public FileHandle readFileHandle(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_READ_FILE_HANDLE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);
        getSerializer().writeTo(byteBufData, sizeOfFile);

        // @formatter:off
        Flux<ByteBuffer> response = this.client
            .requestStream(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)))
            .map(Payload::getData)
//            .map(payload -> {
////                System.out.println("RemoteSenderRSocket.getResource()");
//                ByteBuf byteBuf = payload.data();
//                //payload.retain();
//                return ((NettyDataBufferFactory) getDataBufferFactory()).wrap(byteBuf);
//            })
            //.cast(DataBuffer.class)
            .doOnError(th -> getLogger().error(null, th))
            ;
        // @formatter:on

        return new FileHandleFluxByteBuffer(response);
    }
}
