// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.util.function.LongConsumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import de.freese.jsync.utils.pool.Pool;

/**
 * Uses {@link ByteBuf} and {@link ByteBufPayload}.
 *
 * @author Thomas Freese
 */
public class JSyncRSocketHandlerByteBuf implements RSocket {
    private static final ByteBufAllocator BYTE_BUF_ALLOCATOR = ByteBufAllocator.DEFAULT;
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncRSocketHandlerByteBuf.class);

    private static final Pool<Receiver> POOL_RECEIVER = new Pool<>(true, true) {
        @Override
        protected Receiver create() {
            return new ReceiverDelegateLogger(new LocalhostReceiver());
        }
    };

    private static final Pool<Sender> POOL_SENDER = new Pool<>(true, true) {
        @Override
        protected Sender create() {
            return new SenderDelegateLogger(new LocalhostSender());
        }
    };

    private final Serializer<ByteBuf, ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {
        final Receiver receiver = POOL_RECEIVER.obtain();

        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try {
                final Payload payload = firstSignal.get();
                final ByteBuf bufferMeta = payload.metadata();

                final JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
                getLogger().debug("read command: {}", command);
                RSocketUtils.release(payload);

                return switch (command) {
                    case TARGET_WRITE_FILE -> writeFile(payload, flux.skip(1), receiver);
                    default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
                };
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);

                return Flux.error(ex);
            }
            finally {
                POOL_RECEIVER.free(receiver);
            }
        });
    }

    @Override
    public Mono<Payload> requestResponse(final Payload payload) {
        final Sender sender = POOL_SENDER.obtain();
        final Receiver receiver = POOL_RECEIVER.obtain();

        try {
            final ByteBuf bufferMeta = payload.metadata();

            final JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command) {
                case CONNECT -> connect();
                case DISCONNECT -> disconnect();
                case TARGET_CREATE_DIRECTORY -> createDirectory(payload, receiver);
                case TARGET_DELETE -> delete(payload, receiver);
                case TARGET_UPDATE -> update(payload, receiver);

                default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
            };
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            return Mono.error(ex);
        }
        finally {
            RSocketUtils.release(payload);

            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    @Override
    public Flux<Payload> requestStream(final Payload payload) {
        final Sender sender = POOL_SENDER.obtain();
        final Receiver receiver = POOL_RECEIVER.obtain();

        try {
            final ByteBuf bufferMeta = payload.metadata();

            final JSyncCommand command = getSerializer().readFrom(bufferMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            return switch (command) {
                case SOURCE_CHECKSUM -> checksum(payload, sender);
                case SOURCE_CREATE_SYNC_ITEMS -> generateSyncItems(payload, sender);
                case SOURCE_READ_FILE -> readFile(payload, sender);
                case TARGET_CHECKSUM -> checksum(payload, receiver);
                case TARGET_CREATE_SYNC_ITEMS -> generateSyncItems(payload, receiver);
                case TARGET_VALIDATE_FILE -> validate(payload, receiver);

                default -> throw new IllegalStateException("unknown JSyncCommand: " + command);
            };
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            return Flux.error(ex);
        }
        finally {
            RSocketUtils.release(payload);

            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    protected Serializer<ByteBuf, ByteBuf> getSerializer() {
        return this.serializer;
    }

    private Flux<Payload> checksum(final Payload payload, final FileSystem fileSystem) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final String relativeFile = getSerializer().readFrom(bufferData, String.class);

        return Flux.create(sink -> {
            final LongConsumer consumer = checksumBytesRead -> sink.next(ByteBufPayload.create(Long.toString(checksumBytesRead)));

            final String checksum = fileSystem.generateChecksum(baseDir, relativeFile, consumer);
            sink.next(ByteBufPayload.create(checksum));

            sink.complete();
        });
    }

    private Mono<Payload> connect() {
        final Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload); // .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    private Mono<Payload> createDirectory(final Payload payload, final Receiver receiver) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final String relativePath = getSerializer().readFrom(bufferData, String.class);

        receiver.createDirectory(baseDir, relativePath);

        final Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload); // .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    private Mono<Payload> delete(final Payload payload, final Receiver receiver) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final String relativePath = getSerializer().readFrom(bufferData, String.class);
        final boolean followSymLinks = getSerializer().readFrom(bufferData, Boolean.class);

        receiver.delete(baseDir, relativePath, followSymLinks);

        final Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload); // .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    private Mono<Payload> disconnect() {
        final Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload); // .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    private Flux<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final boolean followSymLinks = getSerializer().readFrom(bufferData, Boolean.class);
        final PathFilter pathFilter = getSerializer().readFrom(bufferData, PathFilter.class);

        return fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).map(syncItem -> {
            final ByteBuf byteBuf = getByteBufAllocator().buffer();
            getSerializer().writeTo(byteBuf, syncItem);
            return byteBuf;
        }).map(ByteBufPayload::create);

        // Consumer<FluxSink<SyncItem>> syncItemConsumer = sink -> {
        // fileSystem.generateSyncItems(baseDir, followSymLinks, sink::next);
        // sink.complete();
        // };
        //
        // return Flux.create(syncItemConsumer).map(syncItem -> {
        // ByteBuf byteBuf = getByteBufAllocator().buffer();
        // getSerializer().writeTo(byteBuf, syncItem);
        // return byteBuf;
        // }).map(ByteBufPayload::create);
    }

    private ByteBufAllocator getByteBufAllocator() {
        return BYTE_BUF_ALLOCATOR;
    }

    private Logger getLogger() {
        return LOGGER;
    }

    private Flux<Payload> readFile(final Payload payload, final Sender sender) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final String relativeFile = getSerializer().readFrom(bufferData, String.class);
        final long sizeOfFile = getSerializer().readFrom(bufferData, Long.class);

        return sender.readFile(baseDir, relativeFile, sizeOfFile)
                .map(DefaultPayload::create)
                ;
    }

    private Mono<Payload> update(final Payload payload, final Receiver receiver) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final SyncItem syncItem = getSerializer().readFrom(bufferData, SyncItem.class);

        receiver.update(baseDir, syncItem);

        final Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload); // .doFinally(signalType -> RSocketUtils.release(responsePayload));
    }

    private Flux<Payload> validate(final Payload payload, final Receiver receiver) {
        return Flux.create(sink -> {
            final ByteBuf bufferData = payload.data();

            try {
                final String baseDir = getSerializer().readFrom(bufferData, String.class);
                final SyncItem syncItem = getSerializer().readFrom(bufferData, SyncItem.class);
                final boolean withChecksum = getSerializer().readFrom(bufferData, Boolean.class);

                final LongConsumer consumer = checksumBytesRead -> sink.next(ByteBufPayload.create(Long.toString(checksumBytesRead)));

                receiver.validateFile(baseDir, syncItem, withChecksum, consumer);
            }
            catch (Exception ex) {
                sink.error(ex);
            }
            finally {
                sink.complete();
            }
        });
    }

    private Flux<Payload> writeFile(final Payload payload, final Flux<Payload> flux, final Receiver receiver) {
        final ByteBuf bufferData = payload.data();

        final String baseDir = getSerializer().readFrom(bufferData, String.class);
        final String relativeFile = getSerializer().readFrom(bufferData, String.class);
        final long sizeOfFile = getSerializer().readFrom(bufferData, Long.class);

        return receiver.writeFile(baseDir, relativeFile, sizeOfFile, flux.map(Payload::getData))
                .map(bytesWritten -> {
                    final ByteBuf data = getByteBufAllocator().buffer().writeLong(bytesWritten);
                    return ByteBufPayload.create(data);
                })
                .doOnError(th -> ByteBufPayload.create(th.getMessage()))
                ;

        // return Flux.concat(response, Mono.just(DefaultPayload.create("TRANSFER COMPLETED"))).onErrorReturn(DefaultPayload.create("FAILED"));
    }
}
