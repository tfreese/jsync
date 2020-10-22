// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.rsocket.utils.RSocketUtils;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class MyRSocketHandler implements RSocket
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRSocketHandler.class);

    /**
    *
    */
    private boolean fail = true;

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
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

                if (metaData != null)
                {
                    String fileName = metaData.getMetadataUtf8();
                    RSocketUtils.release(metaData);

                    // return writeFile1(fileName, flux.skip(1));
                    return writeFile2(fileName, flux.skip(1));
                }

                // return flux;
                return Flux.error(new IllegalStateException("not supported"));
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
        getLogger().debug("received metadata: {}", payload.getMetadataUtf8());

        Mono<Payload> response = null;

        if (this.fail)
        {
            this.fail = false;

            response = Mono.error(new Throwable("Simulated error"));
        }
        else
        {
            Payload pl = ByteBufPayload.create(payload.getDataUtf8() + " from server");

            response = Mono.just(pl).doFinally(signalType -> {
                // RSocketUtils.release(pl);
            });
        }

        RSocketUtils.release(payload);

        return response;
    }

    /**
     * @param fileName String
     * @param payloads {@link Publisher}
     * @return {@link Flux}
     * @throws Exception Falls was schief geht.
     */
    Flux<Payload> writeFile1(final String fileName, final Publisher<Payload> payloads) throws Exception
    {
        Path path = Paths.get(fileName);
        final WritableByteChannel writableByteChannel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        Flux<ByteBuffer> dataFlux = Flux.from(payloads).map(Payload::getData);

        // @formatter:off
        Flux<Payload> response = RSocketUtils.write(dataFlux, writableByteChannel)
                //.map(ByteBufPayload::create)
                .map(byteBuffer -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + byteBuffer.position()))
                .doFinally(signalType -> {
                    RSocketUtils.close(writableByteChannel);
                })
                ;
        // @formatter:on

        // @formatter:off
        return Flux.concat(response, Mono.just(ByteBufPayload.create("COMPLETED")))
                .doOnError(th -> ByteBufPayload.create(th.getMessage()))
                .doOnEach(signal -> RSocketUtils.release(signal.get()))
                ;
        // @formatter:on

        // return Flux.just(ByteBufPayload.create("OK"));
    }

    /**
     * @param fileName String
     * @param payloads {@link Publisher}
     * @return {@link Flux}
     * @throws Exception Falls was schief geht.
     */
    Flux<Payload> writeFile2(final String fileName, final Publisher<Payload> payloads) throws Exception
    {
        Path path = Paths.get(fileName);
        final WritableByteChannel writableByteChannel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        // @formatter:off
        Flux<Payload> response = Flux.from(payloads)
                .map(payload -> {
                    int bytesWritten = RSocketUtils.write(payload, writableByteChannel);

                    return bytesWritten;
                })
                .map(bytesWritten -> ByteBufPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
//                .map(bytesWritten -> ByteBufPayload.create(DefaultPayload.EMPTY_BUFFER))
                .doFinally(signalType -> {
                    RSocketUtils.close(writableByteChannel);
                })
                ;
        // @formatter:on

        // return Flux.concat(response, Mono.just(DefaultPayload.create("COMPLETED"))).onErrorReturn(DefaultPayload.create("FAILED"));

        // @formatter:off
        return Flux.concat(response, Mono.just(ByteBufPayload.create("COMPLETED")))
                .doOnError(th -> ByteBufPayload.create(th.getMessage()))
                .doOnEach(signal -> RSocketUtils.release(signal.get()))
                ;
        // @formatter:on

        // response.subscribe(); // Führt den rest Asynchron aus.
        // response.then().block(); // block()/blockFirst()/blockLast() are blocking, which is not supported in thread
    }

    // // Use Flux.generate to create a publisher that returns file at 1024 bytes
    // // at a time
    // return Flux.generate(
    // sink -> {
    // try {
    // ByteBuffer buffer = ByteBuffer.allocate(1024);
    // int read = channel.read(buffer);
    // buffer.flip();
    // sink.next(DefaultPayload.create(buffer));
    //
    // if (read == -1) {
    // channel.close();
    // sink.complete();
    // }
    // } catch (Throwable t) {
    // sink.error(t);
    // }
    // });
}
