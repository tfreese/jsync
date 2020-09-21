// Created: 20.09.2020
package de.freese.jsync.netty;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.netty.server.JsyncServer;
import de.freese.jsync.utils.JsyncThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestNettyServer
{
    /**
     *
     */
    private static JsyncServer server;

    /**
     * @throws Exception Falls was schief geht.
     */
    @AfterAll
    static void afterAll() throws Exception
    {
        server.stop();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @BeforeAll
    static void beforeAll() throws Exception
    {
        server = new JsyncServer();
        server.start(8005, 2, Executors.newCachedThreadPool(new JsyncThreadFactory("acceptor-")), 4,
                Executors.newCachedThreadPool(new JsyncThreadFactory("worker-")));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testEchoNetty() throws Exception
    {
        NioEventLoopGroup clientLoopGroup = new NioEventLoopGroup(4, Executors.newCachedThreadPool(new JsyncThreadFactory("client-")));

        // @formatter:off
        Bootstrap bootstrap = new Bootstrap()
                .group(clientLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
//                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<io.netty.channel.socket.SocketChannel>()
                {
                    /**
                     * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
                     */
                    @Override
                    public void initChannel(final io.netty.channel.socket.SocketChannel ch) throws Exception
                    {
                        ChannelPipeline p = ch.pipeline();

                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(new ChunkedWriteHandler());
                        p.addLast(new ClientHandler());
                    }
                });
        // @formatter:on

        CountDownLatch latch = new CountDownLatch(1);

        ChannelFuture connectFuture = bootstrap.connect("localhost", 8005).sync();
        connectFuture.addListener(future -> {
            if (future.isSuccess())
            {
                System.out.println("client connected");
                latch.countDown();
            }
            else
            {
                System.out.println("client NOT connected");
                System.err.println(future.cause());
            }
        });

        latch.await();

        Channel channel = connectFuture.channel();
        ChannelFuture closeFuture = channel.closeFuture();

        // Thread.sleep(100); // Etwas Zeit braucht der Client.

        ClientHandler clientHandler = (ClientHandler) channel.pipeline().last();

        Future<String> response = clientHandler.sendMessage("Hello World!");
        System.out.println("Client receive: " + response.get());

        response = clientHandler.sendMessage("Byebye World!");
        System.out.println("Client receive: " + response.get());

        // Warten bis Verbindung beendet.
        // closeFuture.sync();

        clientLoopGroup.shutdownGracefully();

        assertTrue(true, "OK");
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testEchoNIO() throws Exception
    {
        // Thread.sleep(5000);

        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8005);
        SocketChannel channel = SocketChannel.open(serverAddress);
        channel.configureBlocking(true);

        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        Serializers.writeTo(byteBuffer, "Hello World!");
        byteBuffer.flip();

        channel.write(byteBuffer);

        byteBuffer.clear();
        channel.read(byteBuffer);
        byteBuffer.flip();

        System.out.println("Client receive: " + Serializers.readFrom(byteBuffer, String.class));

        channel.shutdownInput();
        channel.shutdownOutput();
        channel.close();

        assertTrue(true, "OK");
    }
}
