// Created: 22.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
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
        connect(uri, tcpClient -> tcpClient.runOn(LoopResources.create("jsync-client-sender-", 3, true)));
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, boolean, java.util.function.Consumer,
     *      java.util.function.LongConsumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final boolean withChecksum, final Consumer<SyncItem> consumerSyncItem,
                                  final LongConsumer consumerBytesRead)
    {
        generateSyncItems(baseDir, followSymLinks, withChecksum, consumerSyncItem, consumerBytesRead, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        ByteBuf byteBufMeta = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufMeta, JSyncCommand.SOURCE_READ_FILE);

        ByteBuf byteBufData = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBufData, baseDir);
        getSerializer().writeTo(byteBufData, relativeFile);
        getSerializer().writeTo(byteBufData, sizeOfFile);

        // @formatter:off
        return getClient()
            .requestStream(Mono.just(ByteBufPayload.create(byteBufData, byteBufMeta)))
            .map(Payload::getData)
            .doOnError(th -> getLogger().error(null, th))
            ;
        // @formatter:on
    }
}
