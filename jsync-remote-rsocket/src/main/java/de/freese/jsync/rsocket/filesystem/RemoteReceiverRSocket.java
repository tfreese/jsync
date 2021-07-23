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
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        connect(uri, tcpClient -> tcpClient.runOn(LoopResources.create("jsync-client-receiver-", 3, true)));
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_CREATE_DIRECTORY);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativePath);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(byteBufData, byteBufMeta)).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
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
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_DELETE);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativePath);
        getSerializer().writeTo(byteBufData, followSymLinks);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(byteBufData, byteBufMeta)).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
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
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer checksumBytesReadConsumer)
    {
        return generateChecksum(baseDir, relativeFile, checksumBytesReadConsumer, JSyncCommand.TARGET_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        generateSyncItems(baseDir, followSymLinks, consumerSyncItem, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_UPDATE);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, syncItem);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(byteBufData, byteBufMeta)).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
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
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_VALIDATE_FILE);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, syncItem);
        getSerializer().writeTo(byteBufData, withChecksum);

        // @formatter:off
        getClient()
            .requestResponse(Mono.just(DefaultPayload.create(byteBufData, byteBufMeta)).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
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
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.TARGET_WRITE_FILE);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);
        getSerializer().writeTo(byteBufData, sizeOfFile);

        Flux<Payload> flux = Flux.concat(Mono.just(DefaultPayload.create(byteBufData, byteBufMeta)).doOnSubscribe(subscription -> {
            getByteBufferPool().free(byteBufMeta);
            getByteBufferPool().free(byteBufData);
        }), fileFlux.map(DefaultPayload::create));

        // @formatter:off
        return getClient()
          .requestChannel(flux)
          .map(payload -> Long.parseLong(payload.getDataUtf8()))
          .doOnError(th -> getLogger().error(null, th))
          ;
      // @formatter:on
    }
}
