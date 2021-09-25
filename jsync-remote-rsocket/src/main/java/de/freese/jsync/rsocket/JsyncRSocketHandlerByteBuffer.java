// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.ReceiverDelegateLogger;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filesystem.SenderDelegateLogger;
import de.freese.jsync.filesystem.local.LocalhostReceiver;
import de.freese.jsync.filesystem.local.LocalhostSender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import de.freese.jsync.utils.pool.Pool;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Verwendet primär {@link ByteBuffer} und somit {@link DefaultPayload}.
 *
 * @author Thomas Freese
 *
 * @deprecated Wirft beim Kopiervorgang eine BufferUnderflowException
 */
@Deprecated
class JsyncRSocketHandlerByteBuffer implements RSocket
{
    /**
    *
    */
    private static final ByteBufferPool byteBufferPool = ByteBufferPool.DEFAULT;

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketHandlerByteBuffer.class);

    /**
    *
    */
    private static final Pool<Receiver> POOL_RECEIVER = new Pool<>(true, true)
    {
        /**
         * @see de.freese.jsync.utils.pool.Pool#create()
         */
        @Override
        protected Receiver create()
        {
            return new ReceiverDelegateLogger(new LocalhostReceiver());
        }
    };

    /**
    *
    */
    private static final Pool<Sender> POOL_SENDER = new Pool<>(true, true)
    {
        /**
         * @see de.freese.jsync.utils.pool.Pool#create()
         */
        @Override
        protected Sender create()
        {
            return new SenderDelegateLogger(new LocalhostSender());
        }
    };
    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Create the checksum.
     *
     * @param payload {@link Payload}
     * @param fileSystem {@link FileSystem}
     *
     * @return {@link Mono}
     */
    private Flux<Payload> checksum(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        String relativeFile = getSerializer().readFrom(bufferData, String.class);

        return Flux.create(sink -> {
            LongConsumer consumer = checksumBytesRead -> sink.next(DefaultPayload.create(Long.toString(checksumBytesRead)));

            String checksum = fileSystem.generateChecksum(baseDir, relativeFile, consumer);
            sink.next(DefaultPayload.create(checksum));

            sink.complete();
        });
    }

    /**
     * @return {@link Mono}
     */
    private Mono<Payload> connect()
    {
        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * Create the Directory.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     *
     * @return {@link Mono}
     */
    private Mono<Payload> createDirectory(final Payload payload, final Receiver receiver)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        String relativePath = getSerializer().readFrom(bufferData, String.class);

        receiver.createDirectory(baseDir, relativePath);

        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * Delete Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     *
     * @return {@link Mono}
     */
    private Mono<Payload> delete(final Payload payload, final Receiver receiver)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        String relativePath = getSerializer().readFrom(bufferData, String.class);
        boolean followSymLinks = getSerializer().readFrom(bufferData, Boolean.class);

        receiver.delete(baseDir, relativePath, followSymLinks);

        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * @return {@link Mono}
     */
    private Mono<Payload> disconnect()
    {
        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * Create the Sync-Items.
     *
     * @param payload {@link Payload}
     * @param fileSystem {@link FileSystem}
     *
     * @return {@link Mono}
     */
    private Flux<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        boolean followSymLinks = getSerializer().readFrom(bufferData, Boolean.class);
        PathFilter pathFilter = getSerializer().readFrom(bufferData, PathFilter.class);

        return fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).map(syncItem -> {
            ByteBuffer buffer = JsyncRSocketHandlerByteBuffer.byteBufferPool.get();
            getSerializer().writeTo(buffer, syncItem);
            return buffer.flip();
        }).map(DefaultPayload::create);

        // Consumer<FluxSink<SyncItem>> syncItemConsumer = sink -> {
        // fileSystem.generateSyncItems(baseDir, followSymLinks, sink::next);
        // sink.complete();
        // };
        //
        // return Flux.create(syncItemConsumer).map(syncItem -> {
        // ByteBuffer buffer = getPooledBuffer();
        // getSerializer().writeTo(buffer, syncItem);
        // return buffer.flip();
        // }).map(DefaultPayload::create);
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @return {@link Serializer}
     */
    protected Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param payload {@link Payload}
     * @param sender {@link Sender}
     *
     * @return {@link Flux}
     */
    private Flux<Payload> readFile(final Payload payload, final Sender sender)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        String relativeFile = getSerializer().readFrom(bufferData, String.class);
        long sizeOfFile = getSerializer().readFrom(bufferData, Long.class);

        Flux<ByteBuffer> fileFlux = sender.readFile(baseDir, relativeFile, sizeOfFile);

        // @formatter:off
        return fileFlux
                .map(DefaultPayload::create)
                ;
        // @formatter:on
    }

    /**
     * @see io.rsocket.RSocket#requestChannel(org.reactivestreams.Publisher)
     */
    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads)
    {
        Receiver receiver = POOL_RECEIVER.obtain();

        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try
            {
                final Payload payload = firstSignal.get();
                ByteBuffer bufferMeta = payload.getMetadata();

                JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
                getLogger().debug("read command: {}", command);
                RSocketUtils.release(payload);

                return switch (command)
                {
                    case TARGET_WRITE_FILE -> writeFile(payload, flux.skip(1), receiver);

                    default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
                };
            }
            catch (Exception ex)
            {
                getLogger().error(null, ex);

                return Flux.error(ex);
            }
            finally
            {
                POOL_RECEIVER.free(receiver);
            }
        });
    }

    /**
     * @see io.rsocket.RSocket#requestResponse(io.rsocket.Payload)
     */
    @Override
    public Mono<Payload> requestResponse(final Payload payload)
    {
        Sender sender = POOL_SENDER.obtain();
        Receiver receiver = POOL_RECEIVER.obtain();

        try
        {
            ByteBuffer bufferMeta = payload.getMetadata();

            JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command)
            {
                case CONNECT -> connect();
                case DISCONNECT -> disconnect();
                case TARGET_CREATE_DIRECTORY -> createDirectory(payload, receiver);
                case TARGET_DELETE -> delete(payload, receiver);
                case TARGET_UPDATE -> update(payload, receiver);
                case TARGET_VALIDATE_FILE -> validate(payload, receiver);

                default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
            };
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Mono.error(ex);
        }
        finally
        {
            RSocketUtils.release(payload);

            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    /**
     * @see io.rsocket.RSocket#requestStream(io.rsocket.Payload)
     */
    @Override
    public Flux<Payload> requestStream(final Payload payload)
    {
        Sender sender = POOL_SENDER.obtain();
        Receiver receiver = POOL_RECEIVER.obtain();

        try
        {
            ByteBuffer bufferMeta = payload.getMetadata();

            JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command)
            {
                case SOURCE_CHECKSUM -> checksum(payload, sender);
                case SOURCE_CREATE_SYNC_ITEMS -> generateSyncItems(payload, sender);
                case SOURCE_READ_FILE -> readFile(payload, sender);
                case TARGET_CHECKSUM -> checksum(payload, receiver);
                case TARGET_CREATE_SYNC_ITEMS -> generateSyncItems(payload, receiver);

                default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
            };
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Flux.error(ex);
        }
        finally
        {
            RSocketUtils.release(payload);

            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    /**
     * Update Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     *
     * @return {@link Mono}
     */
    private Mono<Payload> update(final Payload payload, final Receiver receiver)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        SyncItem syncItem = getSerializer().readFrom(bufferData, SyncItem.class);

        receiver.update(baseDir, syncItem);

        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * Validate Directory or File.
     *
     * @param payload {@link Payload}
     * @param receiver {@link Receiver}
     *
     * @return {@link Mono}
     */
    private Mono<Payload> validate(final Payload payload, final Receiver receiver)
    {
        ByteBuffer bufferData = payload.getMetadata();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        SyncItem syncItem = getSerializer().readFrom(bufferData, SyncItem.class);
        boolean withChecksum = getSerializer().readFrom(bufferData, Boolean.class);

        receiver.validateFile(baseDir, syncItem, withChecksum, null);

        Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    /**
     * Die Daten werden zum Server gesendet.
     *
     * @param payload {@link Payload}
     * @param flux {@link Flux}
     * @param receiver {@link Receiver}
     *
     * @return {@link Flux}
     */
    private Flux<Payload> writeFile(final Payload payload, final Flux<Payload> flux, final Receiver receiver)
    {
        ByteBuffer bufferData = payload.getData();

        String baseDir = getSerializer().readFrom(bufferData, String.class);
        String relativeFile = getSerializer().readFrom(bufferData, String.class);
        long sizeOfFile = getSerializer().readFrom(bufferData, Long.class);

        // @formatter:off
        Flux<Payload> response = receiver.writeFile(baseDir, relativeFile, sizeOfFile, flux.map(Payload::getData))
                .map(bytesWritten -> {
                    ByteBuffer buffer = JsyncRSocketHandlerByteBuffer.byteBufferPool.get();
                    buffer.putLong(bytesWritten).flip();
                    return DefaultPayload.create(buffer);
                })
                .doOnError(th -> DefaultPayload.create(th.getMessage()))
                ;
        // @formatter:on

        return response;

        // return Flux.concat(response, Mono.just(JsyncPayload.create("TRANSFER COMPLETED"))).onErrorReturn(JsyncPayload.create("FAILED"));
    }
}
