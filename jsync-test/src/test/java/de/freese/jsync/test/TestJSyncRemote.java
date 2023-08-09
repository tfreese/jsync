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
import reactor.core.publisher.Hooks;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.EmptyClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.JSyncProtocol;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.nio.server.JSyncNioServer;
import de.freese.jsync.nio.server.handler.JSyncIoHandler;
import de.freese.jsync.rsocket.server.JSyncRSocketServer;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/***
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncRemote extends AbstractJSyncIoTest {
    private static final Map<String, AutoCloseable> CLOSEABLES = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(TestJSyncRemote.class);

    /**
     * @author Thomas Freese
     */
    private static final class TestClientListener extends EmptyClientListener {
        @Override
        public void error(final String message, final Throwable th) {
            assertNull(th);
        }
    }

    private static Options options = null;

    @AfterAll
    static void afterAll() throws Exception {
        for (AutoCloseable closeable : CLOSEABLES.values()) {
            closeable.close();
        }

        LOGGER.info("{}", ByteBufferPool.DEFAULT);

        // if (ByteBufAllocator.DEFAULT instanceof PooledByteBufAllocator)
        // {
        // LOGGER.info(((PooledByteBufAllocator) ByteBufAllocator.DEFAULT).dumpStats());
        // }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        options = new Builder().delete(true).checksum(true).followSymLinks(false).dryRun(false).build();
    }

    @Test
    void testLocal() throws Exception {
        System.out.println();

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    @Test
    void testNio() throws Exception {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerNio(8001);

        URI senderUri = JSyncProtocol.NIO.toUri("localhost:8001", PATH_QUELLE.toString());
        URI receiverUri = JSyncProtocol.NIO.toUri("localhost:8001", PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    @Test
    void testRSocket() throws Exception {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerRSocket(8002);

        URI senderUri = JSyncProtocol.RSOCKET.toUri("localhost:8002", PATH_QUELLE.toString());
        URI receiverUri = JSyncProtocol.RSOCKET.toUri("localhost:8002", PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    private void startServerNio(final int port) throws Exception {
        if (!CLOSEABLES.containsKey("nio")) {
            JSyncNioServer server = new JSyncNioServer(port, 2, 4);
            server.setName("nio");
            server.setIoHandler(new JSyncIoHandler());
            server.start();
            CLOSEABLES.put("nio", server::stop);
        }
    }

    // private void startServerNetty() throws Exception
    // {
    // if (!CLOSEABLES.containsKey("netty"))
    // {
    // JSyncNettyServer server = new JSyncNettyServer();
    // server.start(8002, 2, 4);
    // CLOSEABLES.put("netty", () -> server.stop());
    // }
    // }

    private void startServerRSocket(final int port) throws Exception {
        if (!CLOSEABLES.containsKey("rSocket")) {
            JSyncRSocketServer server = new JSyncRSocketServer();
            server.start(port);
            CLOSEABLES.put("rSocket", server::stop);

            // Fehlermeldungen beim Disconnect ausschalten.
            Hooks.resetOnErrorDropped();
            Hooks.onErrorDropped(th -> {
            });
        }
    }

    // private void startServerSpringRest() throws Exception
    // {
    ////      // @formatter:off
////      new SpringApplicationBuilder(JsyncServerApplication.class)
////              //.properties("server.port=8081") // Does not work, if 'server.port' exist in application.yml.
////              //.run(args);
////              .run(new String[]{"--server.port=8001"});
////      // @formatter:on
    //
    // if (!CLOSEABLES.containsKey("springrest"))
    // {
    // JSyncRestApplication server = new JSyncRestApplication();
    // server.start(new String[]
    // {
    // "--server.port=8003"
    // });
    // CLOSEABLES.put("springrest", () -> server.stop());
    // }
    // }

    // private void startServerSpringWebflux() throws Exception
    // {
    // if (!CLOSEABLES.containsKey("springwebflux"))
    // {
    // JSyncWebfluxApplication server = new JSyncWebfluxApplication();
    // server.start(new String[]
    // {
    // "--server.port=8004"
    // });
    // CLOSEABLES.put("springwebflux", () -> server.stop());
    // }
    // }

    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri) throws Exception {
        Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, PathFilterNoOp.INSTANCE, syncItem -> {
            syncItemsSender.add(syncItem);
            String checksum = client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
                // System.out.println("Sender Bytes read: " + i);
            });
            syncItem.setChecksum(checksum);
        });

        List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, PathFilterNoOp.INSTANCE, syncItem -> {
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

    // @Test
    // void testSpringWebFlux() throws Exception
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
