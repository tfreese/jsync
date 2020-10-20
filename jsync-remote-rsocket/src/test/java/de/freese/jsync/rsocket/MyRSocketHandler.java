// Created: 19.10.2020
package de.freese.jsync.rsocket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
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
        // return writeFile1(payloads);
        return writeFile2(payloads);
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
            Payload pl = DefaultPayload.create(payload.getDataUtf8() + " from server");

            response = Mono.just(pl);
        }

        payload.release();

        return response;
    }

    /**
     * @param payloads {@link Publisher}
     * @return {@link Flux}
     */
    private Flux<Payload> writeFile1(final Publisher<Payload> payloads)
    {
        // @formatter:off
        try
        {
            Flux.from(payloads)
                .map(Payload::getData)
                .subscribe(new WritableByteChannelSubscriber(Paths.get("/tmp", "test.txt")));
            // Kein Subscriber#onComplete -> Channel wird nicht geschlossen!!!
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        // @formatter:on

        return Flux.just(DefaultPayload.create("OK"));
    }

    /**
     * @param payloads {@link Publisher}
     * @return {@link Flux}
     */
    private Flux<Payload> writeFile2(final Publisher<Payload> payloads)
    {
        final AtomicReference<WritableByteChannel> channelReference = new AtomicReference<>(null);

        // @formatter:off
        Flux<Payload> response = Flux.from(payloads)
                .doOnNext(payload -> {
                    if(channelReference.get()==null)
                    {
                        String fileName = payload.getMetadataUtf8();
                        Path path = Paths.get(fileName);

                        try
                        {
                            WritableByteChannel writableByteChannel = Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                            channelReference.set(writableByteChannel);
                        }
                        catch (IOException ex)
                        {
                            throw new UncheckedIOException(ex);
                        }
                    }
                })
                .map(Payload::getData)
                .map(byteBuffer -> {
                    System.out.println("MyRSocketHandler.writeFile2(): write, " + Thread.currentThread().getName());

                    int bytesWritten = 0;

                    try
                    {
                        while (byteBuffer.hasRemaining())
                        {
                            bytesWritten += channelReference.get().write(byteBuffer);
                        }
                    }
                    catch (IOException ex)
                    {
                        throw new UncheckedIOException(ex);
                    }

                    return bytesWritten;
                })
                .map(bytesWritten -> DefaultPayload.create("CHUNK_COMPLETED: bytesWritten = " + bytesWritten))
//                .map(bytesWritten -> DefaultPayload.create(DefaultPayload.EMPTY_BUFFER))
                .doFinally(signalType -> {
                    System.out.println("MyRSocketHandler.writeFile2(): doFinally, " + Thread.currentThread().getName());

                    try
                    {
                        channelReference.get().close();
                        channelReference.set(null);
                    }
                    catch (IOException ex)
                    {
                        throw new UncheckedIOException(ex);
                    }
                })
                ;
        // @formatter:on

        // return Flux.concat(response, Mono.just(DefaultPayload.create("COMPLETED"))).onErrorReturn(DefaultPayload.create("FAILED"));
        return Flux.concat(response, Mono.just(DefaultPayload.create("COMPLETED"))).doOnError(th -> DefaultPayload.create(th.getMessage()));

        // response.subscribe(); // Führt den rest Asynchron aus.
        // response.then().block(); // block()/blockFirst()/blockLast() are blocking, which is not supported in thread

        // System.out.println("MyRSocketHandler.writeFile2(): exit, " + Thread.currentThread().getName());
        // return Flux.concat(Mono.just(DefaultPayload.create("COMPLETED"))).doOnError(th -> DefaultPayload.create(th.getMessage()));
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
