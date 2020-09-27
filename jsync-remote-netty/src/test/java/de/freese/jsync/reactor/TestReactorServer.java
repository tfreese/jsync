// Created: 20.09.2020
package de.freese.jsync.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestReactorServer
{
    /**
     *
     */
    private static JsyncReactorServer server;

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
        server = new JsyncReactorServer();
        server.start(8006, 2, 4);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testEchoNio() throws Exception
    {
        // Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8006);
        SocketChannel channel = SocketChannel.open(serverAddress);
        channel.configureBlocking(true);

        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // serializer.writeTo(byteBuffer, "Hello World!");
        byteBuffer.put("Hello World!".getBytes(StandardCharsets.UTF_8));
        byteBuffer.flip();

        while (byteBuffer.hasRemaining())
        {
            channel.write(byteBuffer);
        }

        byteBuffer.clear();

        ByteBuffer byteBufferResponse = ByteBuffer.allocate(128);

        while (channel.read(byteBuffer) > 0)
        {
            byteBuffer.flip();
            byteBufferResponse.put(byteBuffer);
        }

        byteBuffer = byteBufferResponse.flip();

        // channel.read(byteBuffer);
        // byteBuffer.flip();

        // String response = serializer.readFrom(byteBuffer, String.class);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        String response = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Client receive: " + response);
        assertEquals("Hello World!, from Server", response);

        channel.shutdownInput();
        channel.shutdownOutput();
        channel.close();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testEchoReactor() throws Exception
    {
        LoopResources loop = LoopResources.create("client", 2, 4, true);

        // @formatter:off
        Connection connection = TcpClient
                .create()
                .runOn(loop, true)
                .host("localhost")
                .port(8006)
                //.wiretap(true)
                .option(ChannelOption.TCP_NODELAY, true)
                .doOnConnected(con -> System.out.printf("%s: Client connected%n", Thread.currentThread().getName()))
                .doOnDisconnected(con -> System.out.printf("%s: Client disconnected%n", Thread.currentThread().getName()))
                //.secure(spec -> spec.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)))
                .connectNow()
                ;
        // @formatter:on

        // @formatter:off
        connection
            .outbound()
            .sendString(Mono.just("Hello World!"))
            .then()
            .block()
            ;

        String response = connection
                .inbound()
                .receive()
                .asString(StandardCharsets.UTF_8)
                .blockFirst()
                ;
        // @formatter:on

        System.out.printf("%s: Client receive: %s%n", Thread.currentThread().getName(), response);
        assertEquals("Hello World!, from Server", response);

        // connection.onDispose().block();
        connection.dispose();
    }
}
