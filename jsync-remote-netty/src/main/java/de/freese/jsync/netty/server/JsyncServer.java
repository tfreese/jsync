// Created: 20.09.2020
package de.freese.jsync.netty.server;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Thomas Freese
 */
public class JsyncServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsyncServer.class);

    /**
    *
    */
    private EventLoopGroup acceptorGroup;

    // /**
    // *
    // */
    // private ServerBootstrap bootstrap;

    /**
     *
     */
    private Channel channel;

    /**
    *
    */
    private EventLoopGroup workerGroup;

    /**
     * Erstellt ein neues {@link JsyncServer} Object.
     */
    public JsyncServer()
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
        getLogger().info("starting JsyncServer");

        this.acceptorGroup = new NioEventLoopGroup(acceptorThreads, acceptorExecutor);
        this.workerGroup = new NioEventLoopGroup(workerThreads, workerExecutor);

        // @formatter:off
        ServerBootstrap bootstrap =  new ServerBootstrap()
                .group(this.acceptorGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                //.handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new JsyncServerInitializer());
        // @formatter:on

        try
        {
            ChannelFuture bindFuture = bootstrap.bind(port);
            bindFuture.addListener(future -> {
                if (future.isSuccess())
                {
                    getLogger().info("JsyncServer started");
                }
                else
                {
                    getLogger().error("JsyncServer NOT started", future.cause());
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
        getLogger().info("stopping JsyncServer");

        if (this.acceptorGroup != null)
        {
            this.acceptorGroup.shutdownGracefully();
        }

        if (this.workerGroup != null)
        {
            this.workerGroup.shutdownGracefully();
        }

        getLogger().info("JsyncServer stopped");
    }
}
