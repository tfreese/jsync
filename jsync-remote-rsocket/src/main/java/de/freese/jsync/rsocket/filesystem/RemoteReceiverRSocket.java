// Created: 24.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverRSocket extends AbstractRSocketFileSystem implements Receiver {
    /**
     * Create them only <strong>ONCE</strong>.
     */
    private static final LoopResources LOOP_RESOURCES = LoopResources.create("receiver", 4, true);

    @Override
    public void connect(final URI uri) {
        connect(uri, tcpClient -> tcpClient.runOn(LOOP_RESOURCES));
    }

    @Override
    public void createDirectory(final String baseDir, final String relativePath) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_CREATE_DIRECTORY);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativePath);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(th.getMessage(), th))
            .block()
            ;
        // @formatter:on
    }

    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_DELETE);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativePath);
        getSerializer().writeTo(bufferData, followSymLinks);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(th.getMessage(), th))
            .block()
            ;
        // @formatter:on
    }

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.TARGET_CHECKSUM);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return generateSyncItems(baseDir, followSymLinks, PathFilterNoOp.INSTANCE, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    @Override
    public void update(final String baseDir, final SyncItem syncItem) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_UPDATE);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, syncItem);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(th.getMessage(), th))
            .block()
            ;
        // @formatter:on
    }

    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer consumerChecksumBytesRead) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_VALIDATE_FILE);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, syncItem);
        getSerializer().writeTo(bufferData, withChecksum);

        // @formatter:off
        getClient()
            .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .map(Payload::getDataUtf8)
            .doOnNext(getLogger()::debug)
            .doOnError(th -> getLogger().error(th.getMessage(), th))
            .map(Long::parseLong)
            .subscribe(consumerChecksumBytesRead::accept)
            ;
        // @formatter:on
    }

    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux) {
        final ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_WRITE_FILE);

        final ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativeFile);
        getSerializer().writeTo(bufferData, sizeOfFile);

        // @formatter:off
        final Flux<Payload> flux = Flux.concat(
                Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
                , fileFlux.map(DefaultPayload::create)
                );
        // @formatter:on

        // @formatter:off
        return getClient()
          .requestChannel(flux)
          .map(payload -> payload.data().readLong())
          .doOnError(th -> getLogger().error(th.getMessage(), th))
          ;
       // @formatter:on
    }
}
