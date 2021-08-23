// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.EmptyClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.nio.server.JSyncNioServer;
import de.freese.jsync.nio.server.handler.JSyncIoHandler;
import de.freese.jsync.rsocket.server.JsyncRSocketServer;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import reactor.core.publisher.Hooks;

/***
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncRemote extends AbstractJSyncIoTest
{
    /**
     * @author Thomas Freese
     */
    private static class TestClientListener extends EmptyClientListener
    {
        /**
         * @see de.freese.jsync.client.listener.EmptyClientListener#error(java.lang.String, java.lang.Throwable)
         */
        @Override
        public void error(final String message, final Throwable th)
        {
            assertNull(th);
        }
    }

    /**
    *
    */
    private static final Map<String, AutoCloseable> CLOSEABLES = new HashMap<>();

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJSyncRemote.class);

    /**
    *
    */
    private static Options options = null;

    /**
     * @throws Exception Falls was schief geht.
     */
    @AfterAll
    static void afterAll() throws Exception
    {
        for (AutoCloseable closeable : CLOSEABLES.values())
        {
            closeable.close();
        }

        LOGGER.info("{}", ByteBufferPool.DEFAULT);

        // if (ByteBufAllocator.DEFAULT instanceof PooledByteBufAllocator)
        // {
        // LOGGER.info(((PooledByteBufAllocator) ByteBufAllocator.DEFAULT).dumpStats());
        // }
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @BeforeAll
    static void beforeAll() throws Exception
    {
        options = new Builder().delete(true).checksum(true).followSymLinks(false).dryRun(false).build();
    }

    /**
     * @param port int
     *
     * @throws Exception Falls was schief geht.
     */
    private void startServerNio(final int port) throws Exception
    {
        if (!CLOSEABLES.containsKey("nio"))
        {
            JSyncNioServer server = new JSyncNioServer(port, 2, 4);
            server.setName("nio");
            server.setIoHandler(new JSyncIoHandler());
            server.start();
            CLOSEABLES.put("nio", () -> server.stop());
        }
    }

    /**
     * @param port int
     *
     * @throws Exception Falls was schief geht.
     */
    private void startServerRSocket(final int port) throws Exception
    {
        if (!CLOSEABLES.containsKey("rsocket"))
        {
            JsyncRSocketServer server = new JsyncRSocketServer();
            server.start(port);
            CLOSEABLES.put("rsocket", () -> server.stop());

            // Fehlermeldungen beim Disconnect ausschalten.
            Hooks.resetOnErrorDropped();
            Hooks.onErrorDropped(th -> {
            });
        }
    }

    /**
     * Sync directories.
     *
     * @param options {@link Options} options
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     *
     * @throws Exception Falls was schief geht.
     */
    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri) throws Exception
    {
        Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, null, syncItem -> {
            syncItemsSender.add(syncItem);
            String checksum = client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
                // System.out.println("Sender Bytes read: " + i);
            });
            syncItem.setChecksum(checksum);
        });

        List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, null, syncItem -> {
            syncItemsReceiver.add(syncItem);
            String checksum = client.generateChecksum(EFileSystem.RECEIVER, syncItem, i -> {
                // System.out.println("Sender Bytes read: " + i);
            });
            syncItem.setChecksum(checksum);
        });

        List<SyncPair> syncPairs = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncPairs.forEach(SyncPair::validateStatus);

        client.syncReceiver(syncPairs, new TestClientListener());

        client.disconnectFileSystems();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testLocal() throws Exception
    {
        System.out.println();

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // private void startServerNetty() throws Exception
    // {
    // if (!CLOSEABLES.containsKey("netty"))
    // {
    // JsyncNettyServer server = new JsyncNettyServer();
    // server.start(8002, 2, 4);
    // CLOSEABLES.put("netty", () -> server.stop());
    // }
    // }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNio() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerNio(8001);

        URI senderUri = JSyncUtils.toUri(JSyncProtocol.NIO, "localhost:8001", PATH_QUELLE.toString());
        URI receiverUri = JSyncUtils.toUri(JSyncProtocol.NIO, "localhost:8001", PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // private void startServerSpringRest() throws Exception
    // {
////      // @formatter:off
////      new SpringApplicationBuilder(JsyncServerApplication.class)
////              //.properties("server.port=8081") // Funktioniert nicht, wenn server.port in application.yml enthalten ist.
////              //.run(args);
////              .run(new String[]{"--server.port=8001"});
////      // @formatter:on
    //
    // if (!CLOSEABLES.containsKey("springrest"))
    // {
    // JsyncRestApplication server = new JsyncRestApplication();
    // server.start(new String[]
    // {
    // "--server.port=8003"
    // });
    // CLOSEABLES.put("springrest", () -> server.stop());
    // }
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // private void startServerSpringWebflux() throws Exception
    // {
    // if (!CLOSEABLES.containsKey("springwebflux"))
    // {
    // JsyncWebfluxApplication server = new JsyncWebfluxApplication();
    // server.start(new String[]
    // {
    // "--server.port=8004"
    // });
    // CLOSEABLES.put("springwebflux", () -> server.stop());
    // }
    // }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testRSocket() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerRSocket(8002);

        URI senderUri = JSyncUtils.toUri(JSyncProtocol.RSOCKET, "localhost:8002", PATH_QUELLE.toString());
        URI receiverUri = JSyncUtils.toUri(JSyncProtocol.RSOCKET, "localhost:8002", PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringRest() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
    // URI receiverUri = new URI("jsync://localhost:8003/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);
    //
    // assertTrue(true);
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringWebflux() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
    // URI receiverUri = new URI("jsync://localhost:8003/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_WEB_CLIENT);
    //
    // assertTrue(true);
    // }
}
