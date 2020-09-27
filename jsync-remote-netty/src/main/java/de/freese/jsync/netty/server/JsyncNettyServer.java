// Created: 20.09.2020
package de.freese.jsync.netty.server;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Thomas Freese
 */
public class JsyncNettyServer
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
     * Erstellt ein neues {@link JsyncNettyServer} Object.
     */
    public JsyncNettyServer()
    {
        super();
    }

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
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
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
