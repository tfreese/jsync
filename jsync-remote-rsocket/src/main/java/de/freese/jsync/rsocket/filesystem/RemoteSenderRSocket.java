// Created: 22.10.2020
package de.freese.jsync.rsocket.filesystem;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class RemoteSenderRSocket extends AbstractRSocketFileSystem implements Sender {
    /**
     * Create them only <strong>ONCE</strong>.
     */
    private static final LoopResources LOOP_RESOURCES = LoopResources.create("sender", 4, true);

    @Override
    public void connect(final URI uri) {
        connect(uri, tcpClient -> tcpClient.runOn(LOOP_RESOURCES));
    }

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.SOURCE_CHECKSUM);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return generateSyncItems(baseDir, followSymLinks, pathFilter, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile) {
        ByteBuffer bufferMeta = getByteBufferPool().get();
        getSerializer().writeTo(bufferMeta, JSyncCommand.SOURCE_READ_FILE);

        ByteBuffer bufferData = getByteBufferPool().get();
        getSerializer().writeTo(bufferData, baseDir);
        getSerializer().writeTo(bufferData, relativeFile);
        getSerializer().writeTo(bufferData, sizeOfFile);

        // @formatter:off
        return getClient()
            .requestStream(Mono.just(DefaultPayload.create(bufferData.flip(), bufferMeta.flip()))
                    .doOnSubscribe(subscription -> {
                        getByteBufferPool().free(bufferMeta);
                        getByteBufferPool().free(bufferData);
                    })
            )
            .map(Payload::getData)
            .doOnError(th -> getLogger().error(th.getMessage(), th))
            ;
        // @formatter:on
    }
}
