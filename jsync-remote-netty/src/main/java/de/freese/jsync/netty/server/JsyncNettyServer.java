// Created: 20.09.2020
package de.freese.jsync.netty.server;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.utils.JsyncThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Thomas Freese
 */
public final class JsyncNettyServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncNettyServer.class);

    /**
    *
    */
    private EventLoopGroup acceptorGroup;

    /**
     *
     */
    private Channel channel;

    // /**
    // *
    // */
    // private ServerBootstrap bootstrap;

    /**
    *
    */
    private String name = getClass().getSimpleName();

    /**
    *
    */
    private EventLoopGroup workerGroup;

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param port int
     * @param acceptorThreads int
     * @param acceptorExecutor {@link Executor}
     * @param workerThreads int
     * @param workerExecutor {@link Executor}
     */
    public void start(final int port, final int acceptorThreads, final Executor acceptorExecutor, final int workerThreads, final Executor workerExecutor)
    {
        getLogger().info("starting {}", this.name);

        this.acceptorGroup = new NioEventLoopGroup(acceptorThreads, acceptorExecutor);
        this.workerGroup = new NioEventLoopGroup(workerThreads, workerExecutor);

        // @formatter:off
        ServerBootstrap bootstrap =  new ServerBootstrap()
                .group(this.acceptorGroup, this.workerGroup)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, 64 * 1024)
                .option(ChannelOption.SO_SNDBUF, 64 * 1024)
                .channel(NioServerSocketChannel.class)
                //.handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new JsyncNettyServerInitializer());
        // @formatter:on

        try
        {
            ChannelFuture bindFuture = bootstrap.bind(port);
            bindFuture.addListener(future -> {
                if (future.isSuccess())
                {
                    getLogger().info("{} started", this.name);
                }
                else
                {
                    getLogger().error(this.name + " NOT started", future.cause());
                    return;
                }
            });

            this.channel = bindFuture.channel();

            @SuppressWarnings("unused")
            ChannelFuture closeFuture = this.channel.closeFuture();

            // Warten bis Verbindung beendet.
            // closeFuture.sync();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * @param port int
     * @param acceptorThreads int
     * @param workerThreads int
     */
    public void start(final int port, final int acceptorThreads, final int workerThreads)
    {
        Executor acceptorExecutor = Executors.newCachedThreadPool(new JsyncThreadFactory("acceptor-"));
        Executor workerExecutor = Executors.newCachedThreadPool(new JsyncThreadFactory("worker-"));

        start(port, acceptorThreads, acceptorExecutor, workerThreads, workerExecutor);
    }

    /**
     *
     */
    public void stop()
    {
        getLogger().info("stopping {}", this.name);

        if (this.acceptorGroup != null)
        {
            this.acceptorGroup.shutdownGracefully();
        }

        if (this.workerGroup != null)
        {
            this.workerGroup.shutdownGracefully();
        }

        getLogger().info("{} stopped", this.name);
    }
}
