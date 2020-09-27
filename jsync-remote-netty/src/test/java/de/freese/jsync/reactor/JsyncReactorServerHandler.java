// Created: 27.09.2020
package de.freese.jsync.reactor;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

/**
 * @author Thomas Freese
 */
public class JsyncReactorServerHandler implements BiFunction<NettyInbound, NettyOutbound, Publisher<Void>>
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncReactorServerHandler.class);

    /**
     * Erstellt ein neues {@link JsyncReactorServerHandler} Object.
     */
    public JsyncReactorServerHandler()
    {
        super();
    }

    /**
     * @see java.util.function.BiFunction#apply(java.lang.Object, java.lang.Object)
     */
    @Override
    public Publisher<Void> apply(final NettyInbound inbound, final NettyOutbound outbound)
    {
        // @formatter:off
        return inbound
                .receive()
                .asString(StandardCharsets.UTF_8)
                .doOnNext(value -> getLogger().info("Server read: {}", value))
                .flatMap(value -> outbound.sendString(Mono.just(value + ", from Server")).then())
                .doOnError(throwable -> getLogger().error(null, throwable))
                ;
        // @formatter:on
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }
}
