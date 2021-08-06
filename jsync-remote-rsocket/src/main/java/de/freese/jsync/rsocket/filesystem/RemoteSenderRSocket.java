// Created: 22.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.Sender;
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
public class RemoteSenderRSocket extends AbstractRSocketFileSystem implements Sender
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        connect(uri, tcpClient -> tcpClient.runOn(LoopResources.create("client-sender", 4, true)));
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.SOURCE_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks)
    {
        return generateSyncItems(baseDir, followSymLinks, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumer)
    {
        generateSyncItems(baseDir, followSymLinks, consumer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        ByteBuffer byteBufMeta = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_READ_FILE);

        ByteBuffer byteBufData = getByteBufferPool().obtain();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);
        getSerializer().writeTo(byteBufData, sizeOfFile);

        // @formatter:off
        return getClient()
            .requestStream(Mono.just(DefaultPayload.create(byteBufData.flip(), byteBufMeta.flip())).doOnSubscribe(subscription -> {
                getByteBufferPool().free(byteBufMeta);
                getByteBufferPool().free(byteBufData);
                })
            )
            .map(Payload::getData)
            .doOnError(th -> getLogger().error(null, th))
            ;
        // @formatter:on
    }
}
