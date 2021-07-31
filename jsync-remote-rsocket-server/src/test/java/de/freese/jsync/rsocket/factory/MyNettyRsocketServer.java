// Created: 31.07.2021
package de.freese.jsync.rsocket.factory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rsocket.transport.netty.server.CloseableChannel;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
public class MyNettyRsocketServer implements MyRsocketServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyNettyRsocketServer.class);

    /**
     *
     */
    private CloseableChannel channel;

    /**
     *
     */
    private final Duration lifecycleTimeout;

    /**
     *
     */
    private final Mono<CloseableChannel> starter;

    /**
     * Erstellt ein neues {@link MyNettyRsocketServer} Object.
     *
     * @param starter {@link Mono}
     * @param lifecycleTimeout {@link Duration}
     */
    public MyNettyRsocketServer(final Mono<CloseableChannel> starter, final Duration lifecycleTimeout)
    {
        this.starter = Objects.requireNonNull(starter, "starter required");
        this.lifecycleTimeout = lifecycleTimeout;
    }

    /**
     * @param <T> Type
     * @param mono {@link Mono}
     * @param timeout {@link Duration}
     *
     * @return Object
     */
    protected <T> T block(final Mono<T> mono, final Duration timeout)
    {
        return (timeout != null) ? mono.block(timeout) : mono.block();
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyRsocketServer#getAddress()
     */
    @Override
    public InetSocketAddress getAddress()
    {
        if (this.channel != null)
        {
            return this.channel.address();
        }

        return null;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyRsocketServer#start()
     */
    @Override
    public void start() throws Exception
    {
        this.channel = block(this.starter, this.lifecycleTimeout);

        getLogger().info("Netty RSocket started on port(s): " + getAddress().getPort());

        startDaemonAwaitThread(this.channel);
    }

    /**
     * @param channel {@link CloseableChannel}
     */
    protected void startDaemonAwaitThread(final CloseableChannel channel)
    {
        Thread awaitThread = new Thread(() -> channel.onClose().block(), "rsocket-" + getAddress().getPort());
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    /**
     * @see de.freese.jsync.rsocket.factory.MyRsocketServer#stop()
     */
    @Override
    public void stop()
    {
        if (this.channel != null)
        {
            this.channel.dispose();
            this.channel = null;
        }
    }
}
