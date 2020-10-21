// Created: 19.10.2020
package de.freese.jsync.rsocket.server;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
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
    private final Serializer<ByteBuf> serializer = DefaultSerializer.of(new ByteBufAdapter());

    /**
     * Erstellt ein neues {@link JsyncRSocketHandler} Object.
     */
    public JsyncRSocketHandler()
    {
        super();
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
                final Payload metaData = firstSignal.get();

                JSyncCommand command = getSerializer().readFrom(metaData.metadata(), JSyncCommand.class);
                getLogger().debug("read command: {}", command);

                RSocketUtils.release(metaData);

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
            JSyncCommand command = getSerializer().readFrom(payload.metadata(), JSyncCommand.class);
            getLogger().debug("read command: {}", command);

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
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            return Mono.error(ex);
        }
    }
}
