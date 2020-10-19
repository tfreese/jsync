// Created: 19.10.2020
package de.freese.jsync.rsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
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
