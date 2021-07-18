// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filesystem.local.LocalhostReceiver;
import de.freese.jsync.filesystem.local.LocalhostSender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import de.freese.jsync.utils.ReactiveUtils;
import de.freese.jsync.utils.pool.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
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
    private static final ByteBufAllocator BYTE_BUF_ALLOCATOR = ByteBufAllocator.DEFAULT;

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketHandler.class);

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
            return new LocalhostReceiver();
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
            return new LocalhostSender();
        }
    };

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

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
     *
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
     *
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
     *
     * @return {@link Mono}
     */
    private Mono<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        boolean followSymLinks = getSerializer().readFrom(byteBufData, Boolean.class);
        boolean withChecksum = getSerializer().readFrom(byteBufData, Boolean.class);

        List<SyncItem> syncItems = new ArrayList<>();
        fileSystem.generateSyncItems(baseDir, followSymLinks, withChecksum, syncItems::add, i -> {
        });

        ByteBuf byteBuf = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBuf, syncItems.size());

        syncItems.forEach(syncItem -> getSerializer().writeTo(byteBuf, syncItem));

        return Mono.just(ByteBufPayload.create(byteBuf));// .doFinally(signalType -> byteBuf.release());
    }

    /**
     * @return {@link ByteBufAllocator}
     */
    private ByteBufAllocator getByteBufAllocator()
    {
        return BYTE_BUF_ALLOCATOR;
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
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);
        long sizeOfFile = getSerializer().readFrom(byteBufData, Long.class);

        Flux<ByteBuffer> fileFlux = sender.readFile(baseDir, relativeFile, sizeOfFile);

        // @formatter:off
        return fileFlux
                .map(DefaultPayload::create)
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
        Receiver receiver = POOL_RECEIVER.obtain();

        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try
            {
                final Payload payload = firstSignal.get();
                ByteBuf byteBufMeta = payload.metadata();

                JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
                getLogger().debug("read command: {}", command);

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
            ByteBuf byteBufMeta = payload.metadata();

            JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command)
            {
                case CONNECT -> connect();
                case DISCONNECT -> disconnect();
                case SOURCE_CREATE_SYNC_ITEMS -> generateSyncItems(payload, sender);
                case TARGET_CREATE_SYNC_ITEMS -> generateSyncItems(payload, receiver);
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
            // RSocketUtils.release(payload);

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

        try
        {
            ByteBuf byteBufMeta = payload.metadata();

            JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command)
            {
                case SOURCE_READ_FILE -> readFile(payload, sender);

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
            // RSocketUtils.release(payload);

            POOL_SENDER.free(sender);
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
     *
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
     *
     * @return {@link Flux}
     */
    private Flux<Payload> writeFile(final Payload payload, final Flux<Payload> flux, final Receiver receiver)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);
        long sizeOfFile = getSerializer().readFrom(byteBufData, Long.class);

        // @formatter:off
        Flux<Payload> response = receiver.writeFile(baseDir, relativeFile, sizeOfFile, flux.map(Payload::getData))
                .map(byteBuffer -> {
                    int bytesWritten = byteBuffer.position();
                    ReactiveUtils.release(byteBuffer);
                    return bytesWritten;
                })
                .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
                .doOnError(th -> ByteBufPayload.create(th.getMessage()))
                ;
        // @formatter:on

        return Flux.concat(response, Mono.just(ByteBufPayload.create("TRANSFER COMPLETED")));

        // return Flux.concat(response, Mono.just(DefaultPayload.create("TRANSFER COMPLETED"))).onErrorReturn(DefaultPayload.create("FAILED"));
    }
}
