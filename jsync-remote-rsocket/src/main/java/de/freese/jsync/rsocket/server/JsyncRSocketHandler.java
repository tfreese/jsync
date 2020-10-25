// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.fileHandle.FileHandle;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class JsyncRSocketHandler implements RSocket
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketHandler.class);

    /**
    *
    */
    private static final ThreadLocal<Receiver> THREAD_LOCAL_RECEIVER = ThreadLocal.withInitial(LocalhostReceiver::new);

    /**
        *
        */
    private static final ThreadLocal<Sender> THREAD_LOCAL_SENDER = ThreadLocal.withInitial(LocalhostSender::new);

    /**
    *
    */
    private final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link JsyncRSocketHandler} Object.
     */
    public JsyncRSocketHandler()
    {
        super();
    }

    /**
     * Create the checksum.
     *
     * @param payload {@link Payload}
     * @param fileSystem {@link FileSystem}
     * @return {@link Mono}
     */
    private Mono<Payload> checksum(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);

        String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
        });

        return Mono.just(ByteBufPayload.create(checksum));
    }

    /**
     * @return {@link Mono}
     */
    private Mono<Payload> connect()
    {
        Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload);// .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    /**
     * Create the Directory.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     * @return {@link Mono}
     */
    private Mono<Payload> createDirectory(final Payload payload, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativePath = getSerializer().readFrom(byteBufData, String.class);

        receiver.createDirectory(baseDir, relativePath);

        return Mono.just(ByteBufPayload.create("OK"));
    }

    /**
     * Delete Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     * @return {@link Mono}
     */
    private Mono<Payload> delete(final Payload payload, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativePath = getSerializer().readFrom(byteBufData, String.class);
        boolean followSymLinks = getSerializer().readFrom(byteBufData, Boolean.class);

        receiver.delete(baseDir, relativePath, followSymLinks);

        return Mono.just(ByteBufPayload.create("OK"));
    }

    /**
     * @return {@link Mono}
     */
    private Mono<Payload> disconnect()
    {
        Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload);// .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    /**
     * Create the Sync-Items.
     *
     * @param payload {@link Payload}
     * @param fileSystem {@link FileSystem}
     * @return {@link Mono}
     */
    private Mono<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        boolean followSymLinks = getSerializer().readFrom(byteBufData, Boolean.class);

        List<SyncItem> syncItems = new ArrayList<>(128);

        fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("SyncItem generated: {}", syncItem);

            syncItems.add(syncItem);
        });

        ByteBuf byteBuf = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBuf, syncItems.size());

        for (SyncItem syncItem : syncItems)
        {
            getSerializer().writeTo(byteBuf, syncItem);
        }

        return Mono.just(ByteBufPayload.create(byteBuf));// .doFinally(signalType -> byteBuf.release());
    }

    /**
     * @return {@link ByteBufAllocator}
     */
    private ByteBufAllocator getByteBufAllocator()
    {
        return this.byteBufAllocator;
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @return {@link Serializer}<ByteBuf>
     */
    protected Serializer<ByteBuf> getSerializer()
    {
        return this.serializer;
    }

    // /**
    // * Read File-Chunk from Sender.
    // *
    // * @param payload {@link Payload}
    // * @param sender {@link Sender}
    // * @return {@link Mono}
    // */
    // private Mono<Payload> readChunk(final Payload payload, final Sender sender)
    // {
    // ByteBuf byteBufData = payload.data();
    //
    // String baseDir = getSerializer().readFrom(byteBufData, String.class);
    // String relativeFile = getSerializer().readFrom(byteBufData, String.class);
    // long position = getSerializer().readFrom(byteBufData, Long.class);
    // long sizeOfChunk = getSerializer().readFrom(byteBufData, Long.class);
    //
    // ByteBuf byteBuf = getByteBufAllocator().buffer((int) sizeOfChunk);
    // ByteBuffer byteBuffer = byteBuf.nioBuffer(0, (int) sizeOfChunk);
    //
    // sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);
    //
    // return Mono.just(ByteBufPayload.create(byteBuf));
    // }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param payload {@link Payload}
     * @param sender {@link Sender}
     * @return {@link Flux}
     */
    private Flux<Payload> readFileHandle(final Payload payload, final Sender sender)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);
        long sizeOfFile = getSerializer().readFrom(byteBufData, Long.class);

        FileHandle fileHandle = sender.readFileHandle(baseDir, relativeFile, sizeOfFile);

        Flux<DataBuffer> flux =
                DataBufferUtils.readByteChannel(fileHandle::getHandle, new NettyDataBufferFactory(getByteBufAllocator()), Options.DATABUFFER_SIZE);

        // @formatter:off
        return flux
                .cast(NettyDataBuffer.class)
                .map(dataBuffer -> {
                    Payload pl = ByteBufPayload.create(dataBuffer.getNativeBuffer());
                    //dataBuffer.retain();
                    return pl;
                })
                //.doFinally(signalType -> dataBuffer.release())
                ;
        // @formatter:on
    }

    /**
     * @see io.rsocket.RSocket#requestChannel(org.reactivestreams.Publisher)
     */
    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads)
    {
        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try
            {
                final Payload payload = firstSignal.get();
                ByteBuf byteBufMeta = payload.metadata();

                JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
                getLogger().debug("read command: {}", command);

                // RSocketUtils.release(payload);

                switch (command)
                {
                    case TARGET_WRITE_FILE_HANDLE:
                        return writeFileHandle(payload, flux.skip(1), THREAD_LOCAL_RECEIVER.get());
                    default:
                        throw new IllegalStateException("unknown JSyncCommand: " + command);
                }
            }
            catch (Exception ex)
            {
                getLogger().error(null, ex);

                return Flux.error(ex);
            }
        });
    }

    /**
     * @see io.rsocket.RSocket#requestResponse(io.rsocket.Payload)
     */
    @Override
    public Mono<Payload> requestResponse(final Payload payload)
    {
        try
        {
            ByteBuf byteBufMeta = payload.metadata();

            JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            switch (command)
            {
                case CONNECT:
                    return connect();

                case DISCONNECT:
                    return disconnect();

                case SOURCE_CREATE_SYNC_ITEMS:
                    return generateSyncItems(payload, THREAD_LOCAL_SENDER.get());

                case SOURCE_CHECKSUM:
                    return checksum(payload, THREAD_LOCAL_SENDER.get());

                // case SOURCE_READ_CHUNK:
                // return readChunk(payload, THREAD_LOCAL_SENDER.get());

                case TARGET_CREATE_SYNC_ITEMS:
                    return generateSyncItems(payload, THREAD_LOCAL_RECEIVER.get());

                case TARGET_CHECKSUM:
                    return checksum(payload, THREAD_LOCAL_RECEIVER.get());

                case TARGET_CREATE_DIRECTORY:
                    return createDirectory(payload, THREAD_LOCAL_RECEIVER.get());

                case TARGET_DELETE:
                    return delete(payload, THREAD_LOCAL_RECEIVER.get());

                case TARGET_UPDATE:
                    return update(payload, THREAD_LOCAL_RECEIVER.get());

                case TARGET_VALIDATE_FILE:
                    return validate(payload, THREAD_LOCAL_RECEIVER.get());

                // case TARGET_WRITE_CHUNK:
                // return writeChunk(payload, THREAD_LOCAL_RECEIVER.get());

                default:
                    throw new IllegalStateException("unknown JSyncCommand: " + command);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Mono.error(ex);
        }
        finally
        {
            // RSocketUtils.release(payload);
        }
    }

    /**
     * @see io.rsocket.RSocket#requestStream(io.rsocket.Payload)
     */
    @Override
    public Flux<Payload> requestStream(final Payload payload)
    {
        try
        {
            ByteBuf byteBufMeta = payload.metadata();

            JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            switch (command)
            {
                case SOURCE_READ_FILE_HANDLE:
                    return readFileHandle(payload, THREAD_LOCAL_SENDER.get());

                default:
                    throw new IllegalStateException("unknown JSyncCommand: " + command);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Flux.error(ex);
        }
        finally
        {
            // RSocketUtils.release(payload);
        }
    }

    /**
     * Update Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     * @return {@link Mono}
     */
    private Mono<Payload> update(final Payload payload, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        SyncItem syncItem = getSerializer().readFrom(byteBufData, SyncItem.class);

        receiver.update(baseDir, syncItem);

        return Mono.just(ByteBufPayload.create("OK"));
    }

    /**
     * Validate Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     * @return {@link Mono}
     */
    private Mono<Payload> validate(final Payload payload, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        SyncItem syncItem = getSerializer().readFrom(byteBufData, SyncItem.class);
        boolean withChecksum = getSerializer().readFrom(byteBufData, Boolean.class);

        receiver.validateFile(baseDir, syncItem, withChecksum);

        return Mono.just(ByteBufPayload.create("OK"));
    }

    /**
     * Die Daten werden zum Server gesendet.
     *
     * @param payload {@link Payload}
     * @param flux {@link Flux}
     * @param receiver {@link Receiver}
     * @return {@link Flux}
     */
    private Flux<Payload> writeFileHandle(final Payload payload, final Flux<Payload> flux, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);
        // long sizeOfFile = getSerializer().readFrom(byteBufData, Long.class);

        Path path = Paths.get(baseDir, relativeFile);
        Path parentPath = path.getParent();

        final Flux<Payload> response;

        // FileHandleFluxByteBuffer fileHandle = new FileHandleFluxByteBuffer(flux.map(Payload::getData));

        try
        {
            // FileHandle fileHandle = new FileHandle()
            // {
            // /**
            // * @see de.freese.jsync.filesystem.fileHandle.FileHandle#writeTo(java.nio.channels.WritableByteChannel, long)
            // */
            // @Override
            // public <T> T writeTo(final WritableByteChannel writableByteChannel, final long sizeOfFile) throws Exception
            // {
            // response = RSocketUtils.write(flux.map(Payload::getData), writableByteChannel).map(ByteBuffer::position)
            // .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
            // .doOnError(th -> ByteBufPayload.create(th.getMessage())).doFinally(signalType -> {
            // RSocketUtils.close(writableByteChannel);
            // });
            //
            // return null;
            // }
            // };
            //
            // receiver.writeFileHandle(baseDir, relativeFile, sizeOfFile, fileHandle, bytesWritten -> {
            // });

            if (Files.notExists(parentPath))
            {
                Files.createDirectories(parentPath);
            }

            if (Files.notExists(path))
            {
                Files.createFile(path);
            }

            final FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // @formatter:off
            response = RSocketUtils.write(flux.map(Payload::getData), fileChannel)
                    .map(ByteBuffer::position)
                    .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
                    .doOnError(th -> ByteBufPayload.create(th.getMessage()))
                    .doFinally(signalType -> {
                        RSocketUtils.close(fileChannel);
                    });
            // @formatter:on

//            // @formatter:off
//            response = Flux.from(flux)
//                    .map(pl -> {
//                        int bytesWritten = RSocketUtils.write(pl, fileChannel);
//                        return bytesWritten;
//                    })
//                    .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
//                    .doOnError(th -> ByteBufPayload.create(th.getMessage()))
//                    .doFinally(signalType -> {
//                        RSocketUtils.close(fileChannel);
//                    })
//                    ;
//            // @formatter:on

//            // @formatter:off
//             response = flux
//                     .map(Payload::getData)
//                     .map(byteBuffer -> {
//                         int bytesWritten = 0;
//
//                         try
//                            {
//                                 while (byteBuffer.hasRemaining())
//                                 {
//                                     bytesWritten += fileChannel.write(byteBuffer);
//                                 }
//                            }
//                            catch (IOException ex)
//                            {
//                                throw new UncheckedIOException(ex);
//                            }
//
//
//                         return bytesWritten;
//                     })
//                     .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
//                     .doOnError(th -> ByteBufPayload.create(th.getMessage()))
//                     .doFinally(signalType -> {
//                         fileChannel.force(false);
//                         RSocketUtils.close(fileChannel);
//                     })
//                     ;
//             // @formatter:on
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        // @formatter:off
        return Flux.concat(response, Mono.just(ByteBufPayload.create("TRANSFER COMPLETED")));
        // @formatter:on

        // return Flux.concat(response, Mono.just(DefaultPayload.create("TRANSFER COMPLETED"))).onErrorReturn(DefaultPayload.create("FAILED"));
        //
//        // @formatter:off
//        return Flux.concat(response, Mono.just(ByteBufPayload.create("TRANSFER COMPLETED")))
//                .doOnError(th -> ByteBufPayload.create(th.getMessage()))
//                .doOnEach(signal -> RSocketUtils.release(signal.get()))
//                ;

//        return Flux.just(ByteBufPayload.create("OK"));
    }

//    /**
//     * Write File-Chunk to Receiver.
//     *
//     * @param payload {@link Payload}
//     * @param receiver {@link Receiver}
//     * @return {@link Mono}
//     */
//    private Mono<Payload> writeChunk(final Payload payload, final Receiver receiver)
//    {
//        ByteBuf byteBufData = payload.data();
//
//        String baseDir = getSerializer().readFrom(byteBufData, String.class);
//        String relativeFile = getSerializer().readFrom(byteBufData, String.class);
//        long position = getSerializer().readFrom(byteBufData, Long.class);
//        long sizeOfChunk = getSerializer().readFrom(byteBufData, Long.class);
//
//        ByteBuffer byteBuffer = byteBufData.nioBuffer();
//
//        receiver.writeChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);
//
//        return Mono.just(ByteBufPayload.create("OK"));
//    }
}
