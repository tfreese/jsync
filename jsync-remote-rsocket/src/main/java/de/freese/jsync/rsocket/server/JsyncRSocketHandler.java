// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncRSocketHandler.class);

    /**
    *
    */
    private static final ThreadLocal<Receiver> THREAD_LOCAL_RECEIVER = ThreadLocal.withInitial(LocalhostReceiver::new);

    /**
        *
        */
    private static final ThreadLocal<Sender> THREAD_LOCAL_SENDER = ThreadLocal.withInitial(LocalhostSender::new);

    /**
    *
    */
    private final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;

    /**
    *
    */
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link JsyncRSocketHandler} Object.
     */
    public JsyncRSocketHandler()
    {
        super();
    }

    /**
     * Create the checksum.
     *
     * @param payload {@link Payload}
     * @param fileSystem {@link FileSystem}
     * @return {@link Mono}
     */
    private Mono<Payload> checksum(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        String relativeFile = getSerializer().readFrom(byteBufData, String.class);

        String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
        });

        return Mono.just(ByteBufPayload.create(checksum));
    }

    /**
     * @return {@link Mono}
     */
    private Mono<Payload> connect()
    {
        Payload responsePayload = ByteBufPayload.create("OK");

        return Mono.just(responsePayload);// .doFinally(signalType -> RSocketUtils.release(responsePayload));
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
     * @return {@link Mono}
     */
    private Mono<Payload> generateSyncItems(final Payload payload, final FileSystem fileSystem)
    {
        ByteBuf byteBufData = payload.data();

        String baseDir = getSerializer().readFrom(byteBufData, String.class);
        boolean followSymLinks = getSerializer().readFrom(byteBufData, Boolean.class);

        List<SyncItem> syncItems = new ArrayList<>(128);

        fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("SyncItem generated: {}", syncItem);

            syncItems.add(syncItem);
        });

        ByteBuf byteBuf = getByteBufAllocator().buffer();
        getSerializer().writeTo(byteBuf, syncItems.size());

        for (SyncItem syncItem : syncItems)
        {
            getSerializer().writeTo(byteBuf, syncItem);
        }

        return Mono.just(ByteBufPayload.create(byteBuf));// .doFinally(signalType -> byteBuf.release());
    }

    /**
     * @return {@link ByteBufAllocator}
     */
    private ByteBufAllocator getByteBufAllocator()
    {
        return this.byteBufAllocator;
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
     * @see io.rsocket.RSocket#requestChannel(org.reactivestreams.Publisher)
     */
    @Override
    public Flux<Payload> requestChannel(final Publisher<Payload> payloads)
    {
        return Flux.from(payloads).switchOnFirst((firstSignal, flux) -> {
            try
            {
                final Payload payloadMeta = firstSignal.get();
                ByteBuf byteBufMeta = payloadMeta.metadata();

                JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
                getLogger().debug("read command: {}", command);

                RSocketUtils.release(payloadMeta);

                switch (command)
                {
                    // case SOURCE_READ_CHUNK:
                    // return readChunk(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                    //
                    // case SOURCE_READABLE_RESOURCE:
                    // return resourceReadable(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                    //
                    // case TARGET_WRITE_CHUNK:
                    // return writeChunk(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    //
                    // case TARGET_WRITEABLE_RESOURCE:
                    // return resourceWritable(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    default:
                        throw new IllegalStateException("unknown JSyncCommand: " + command);
                }

                // // return writeFile1(fileName, flux.skip(1));
                // return writeFile2(fileName, flux.skip(1));
            }
            catch (Exception ex)
            {
                getLogger().error(null, ex);

                return Flux.error(ex);
            }
        });
    }

    /**
     * @see io.rsocket.RSocket#requestResponse(io.rsocket.Payload)
     */
    @Override
    public Mono<Payload> requestResponse(final Payload payload)
    {
        try
        {
            ByteBuf byteBufMeta = payload.metadata();

            JSyncCommand command = getSerializer().readFrom(byteBufMeta, JSyncCommand.class);
            getLogger().debug("read command: {}", command);

            switch (command)
            {
                case CONNECT:
                    return connect();

                case DISCONNECT:
                    return disconnect();

                case SOURCE_CREATE_SYNC_ITEMS:
                    return generateSyncItems(payload, THREAD_LOCAL_SENDER.get());

                case SOURCE_CHECKSUM:
                    return checksum(payload, THREAD_LOCAL_SENDER.get());

                default:
                    throw new IllegalStateException("unknown JSyncCommand: " + command);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Mono.error(ex);
        }
        finally
        {
            RSocketUtils.release(payload);
        }
    }
}
