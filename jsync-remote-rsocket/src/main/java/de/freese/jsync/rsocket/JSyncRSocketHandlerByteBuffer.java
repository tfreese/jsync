// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import io.rsocket.Payload;
import io.rsocket.RSocket;
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
import de.freese.jsync.rsocket.utils.RSocketUtils;
import de.freese.jsync.serialisation.DefaultSerializer;
import de.freese.jsync.serialisation.Serializer;
import de.freese.jsync.serialisation.io.ByteBufferReader;
import de.freese.jsync.serialisation.io.ByteBufferWriter;
import de.freese.jsync.utils.pool.Pool;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * Uses {@link ByteBuffer} and {@link DefaultPayload}.
 *
 * @author Thomas Freese
 * @deprecated Throws a BufferUnderflowException during copy process
 */
@Deprecated
class JSyncRSocketHandlerByteBuffer implements RSocket {
    private static final ByteBufferPool BYTEBUFFER_POOL = ByteBufferPool.DEFAULT;
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncRSocketHandlerByteBuffer.class);

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
    private final Serializer<ByteBuffer, ByteBuffer> serializer = new DefaultSerializer<>(new ByteBufferReader(), new ByteBufferWriter());

    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {
        final Receiver receiver = POOL_RECEIVER.obtain();

        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try {
                final Payload payload = firstSignal.get();
                final ByteBuffer bufferMeta = payload.getMetadata();

                final JSyncCommand command = getSerializer().readJSyncCommand(bufferMeta);
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
            final ByteBuffer bufferMeta = payload.getMetadata();

            final JSyncCommand command = getSerializer().readJSyncCommand(bufferMeta);
            getLogger().debug("read command: {}", command);

            return switch (command) {
                case CONNECT -> connect();
                case DISCONNECT -> disconnect();
                case TARGET_CREATE_DIRECTORY -> createDirectory(payload, receiver);
                case TARGET_DELETE -> delete(payload, receiver);
                case TARGET_UPDATE -> update(payload, receiver);
                case TARGET_VALIDATE_FILE -> validate(payload, receiver);
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
            final ByteBuffer bufferMeta = payload.getMetadata();

            final JSyncCommand command = getSerializer().readJSyncCommand(bufferMeta);
            getLogger().debug("read command: {}", command);

            return switch (command) {
                case SOURCE_CHECKSUM -> checksum(payload, sender);
                case SOURCE_CREATE_SYNC_ITEMS -> generateSyncItems(payload, sender);
                case SOURCE_READ_FILE -> readFile(payload, sender);
                case TARGET_CHECKSUM -> checksum(payload, receiver);
                case TARGET_CREATE_SYNC_ITEMS -> generateSyncItems(payload, receiver);
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

    protected Serializer<ByteBuffer, ByteBuffer> getSerializer() {
        return this.serializer;
    }

    private Flux<Payload> checksum(final Payload payload, final FileSystem fileSystem) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final String relativeFile = getSerializer().readString(bufferData);

        return Flux.create(sink -> {
            final LongConsumer consumer = checksumBytesRead -> sink.next(DefaultPayload.create(Long.toString(checksumBytesRead)));

            final String checksum = fileSystem.generateChecksum(baseDir, relativeFile, consumer);
            sink.next(DefaultPayload.create(checksum));

            sink.complete();
        });
    }

    private Mono<Payload> connect() {
        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Mono<Payload> createDirectory(final Payload payload, final Receiver receiver) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final String relativePath = getSerializer().readString(bufferData);

        receiver.createDirectory(baseDir, relativePath);

        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Mono<Payload> delete(final Payload payload, final Receiver receiver) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final String relativePath = getSerializer().readString(bufferData);
        final boolean followSymLinks = getSerializer().readBoolean(bufferData);

        receiver.delete(baseDir, relativePath, followSymLinks);

        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Mono<Payload> disconnect() {
        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Flux<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final boolean followSymLinks = getSerializer().readBoolean(bufferData);
        final PathFilter pathFilter = getSerializer().readPathFilter(bufferData);

        return fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).map(syncItem -> {
            final ByteBuffer buffer = JSyncRSocketHandlerByteBuffer.BYTEBUFFER_POOL.get();
            getSerializer().write(buffer, syncItem);
            return buffer.flip();
        }).map(DefaultPayload::create);

        // Consumer<FluxSink<SyncItem>> syncItemConsumer = sink -> {
        // fileSystem.generateSyncItems(baseDir, followSymLinks, sink::next);
        // sink.complete();
        // };
        //
        // return Flux.create(syncItemConsumer).map(syncItem -> {
        // ByteBuffer buffer = getPooledBuffer();
        // getSerializer().write(buffer, syncItem);
        // return buffer.flip();
        // }).map(DefaultPayload::create);
    }

    private Logger getLogger() {
        return LOGGER;
    }

    private Flux<Payload> readFile(final Payload payload, final Sender sender) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final String relativeFile = getSerializer().readString(bufferData);
        final long sizeOfFile = getSerializer().readLong(bufferData);

        return sender.readFile(baseDir, relativeFile, sizeOfFile)
                .map(DefaultPayload::create)
                ;
    }

    private Mono<Payload> update(final Payload payload, final Receiver receiver) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final SyncItem syncItem = getSerializer().readSyncItem(bufferData);

        receiver.update(baseDir, syncItem);

        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Mono<Payload> validate(final Payload payload, final Receiver receiver) {
        final ByteBuffer bufferData = payload.getMetadata();

        final String baseDir = getSerializer().readString(bufferData);
        final SyncItem syncItem = getSerializer().readSyncItem(bufferData);
        final boolean withChecksum = getSerializer().readBoolean(bufferData);

        receiver.validateFile(baseDir, syncItem, withChecksum, null);

        final Payload responsePayload = DefaultPayload.create("OK");

        return Mono.just(responsePayload);
    }

    private Flux<Payload> writeFile(final Payload payload, final Flux<Payload> flux, final Receiver receiver) {
        final ByteBuffer bufferData = payload.getData();

        final String baseDir = getSerializer().readString(bufferData);
        final String relativeFile = getSerializer().readString(bufferData);
        final long sizeOfFile = getSerializer().readLong(bufferData);

        return receiver.writeFile(baseDir, relativeFile, sizeOfFile, flux.map(Payload::getData))
                .map(bytesWritten -> {
                    final ByteBuffer buffer = JSyncRSocketHandlerByteBuffer.BYTEBUFFER_POOL.get();
                    buffer.putLong(bytesWritten).flip();
                    return DefaultPayload.create(buffer);
                })
                .doOnError(th -> DefaultPayload.create(th.getMessage()))
                ;

        // return Flux.concat(response, Mono.just(JSyncPayload.create("TRANSFER COMPLETED"))).onErrorReturn(JSyncPayload.create("FAILED"));
    }
}
