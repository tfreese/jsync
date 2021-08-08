// Created: 24.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverRSocket extends AbstractRSocketFileSystem implements Receiver
{
    /**
     * Dürfen nur einmal erzeugt werden.
     */
    private static final LoopResources LOOP_RESOURCES = LoopResources.create("receiver", 4, true);

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        connect(uri, tcpClient -> tcpClient.runOn(LOOP_RESOURCES));
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        ByteBuffer bufferMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_CREATE_DIRECTORY);

        ByteBuffer bufferData = getByteBufferPool().obtain();
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
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        ByteBuffer bufferMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_DELETE);

        ByteBuffer bufferData = getByteBufferPool().obtain();
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
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.TARGET_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks)
    {
        return generateSyncItems(baseDir, followSymLinks, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumer)
    {
        generateSyncItems(baseDir, followSymLinks, consumer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        ByteBuffer bufferMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_UPDATE);

        ByteBuffer bufferData = getByteBufferPool().obtain();
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
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean, java.util.function.LongConsumer)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer checksumBytesReadConsumer)
    {
        ByteBuffer bufferMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_VALIDATE_FILE);

        ByteBuffer bufferData = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, syncItem);
        getSerializer().writeTo(bufferData, withChecksum);

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
            .doOnError(th -> getLogger().error(null, th))
            .block()
            ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#writeFile(java.lang.String, java.lang.String, long, reactor.core.publisher.Flux)
     */
    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux)
    {
        ByteBuffer bufferMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferMeta, JSyncCommand.TARGET_WRITE_FILE);

        ByteBuffer bufferData = getByteBufferPool().obtain();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativeFile);
        getSerializer().writeTo(bufferData, sizeOfFile);

        // @formatter:off
        Flux<Payload> flux = Flux.concat(
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
          .doOnError(th -> getLogger().error(null, th))
          ;
       // @formatter:on
    }
}
