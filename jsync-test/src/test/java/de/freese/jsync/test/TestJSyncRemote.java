// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/***
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncRemote extends AbstractJSyncIoTest {
    private static final Map<String, AutoCloseable> CLOSEABLES = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJSyncRemote.class);
    private static final Path PATH_DEST = createDestPath(TestJSyncRemote.class);
    private static final Path PATH_SOURCE = createSourcePath(TestJSyncRemote.class);

    private static Options options;

    /**
     * @author Thomas Freese
     */
    private static final class TestClientListener extends EmptyClientListener {
        @Override
        public void error(final String message, final Throwable th) {
            assertNull(th);
        }
    }

    @AfterAll
    static void afterAll() {
        for (AutoCloseable closeable : CLOSEABLES.values()) {
            try {
                closeable.close();
            }
            catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        // if (ByteBufAllocator.DEFAULT instanceof PooledByteBufAllocator) {
        // LOGGER.info(((PooledByteBufAllocator) ByteBufAllocator.DEFAULT).dumpStats());
        // }
    }

    @BeforeAll
    static void beforeAll() {
        options = new Builder().delete(true).checksum(true).followSymLinks(false).dryRun(false).build();
    }

    @AfterEach
    void afterEach() throws Exception {
        deletePaths(PATH_SOURCE, PATH_DEST);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        createSourceStructure(PATH_SOURCE);
    }

    @Test
    void testLocal() throws Exception {
        final URI senderUri = PATH_SOURCE.toUri();
        final URI receiverUri = PATH_DEST.toUri();

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    @Test
    void testNio() throws Exception {
        startServerNio(8001);

        final URI senderUri = JSyncProtocol.NIO.toUri("localhost:8001", PATH_SOURCE.toString());
        final URI receiverUri = JSyncProtocol.NIO.toUri("localhost:8001", PATH_DEST.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    @Test
    void testRSocket() throws Exception {
        TimeUnit.MILLISECONDS.sleep(500);

        startServerRSocket(8002);

        final URI senderUri = JSyncProtocol.RSOCKET.toUri("localhost:8002", PATH_SOURCE.toString());
        final URI receiverUri = JSyncProtocol.RSOCKET.toUri("localhost:8002", PATH_DEST.toString());

        syncDirectories(options, senderUri, receiverUri);

        assertTrue(true);
    }

    private void startServerNio(final int port) {
        if (!CLOSEABLES.containsKey("nio")) {
            final JSyncNioServer server = new JSyncNioServer(port, 2, 4);
            server.setName("nio");
            server.setIoHandler(new JSyncIoHandler());
            server.start();
            CLOSEABLES.put("nio", server::stop);
        }
    }

    // private void startServerNetty() {
    // if (!CLOSEABLES.containsKey("netty")) {
    // JSyncNettyServer server = new JSyncNettyServer();
    // server.start(8002, 2, 4);
    // CLOSEABLES.put("netty", () -> server.stop());
    // }
    // }

    private void startServerRSocket(final int port) {
        if (!CLOSEABLES.containsKey("rSocket")) {
            final JSyncRSocketServer server = new JSyncRSocketServer();
            server.start(port);
            CLOSEABLES.put("rSocket", server::stop);

            // Fehlermeldungen beim Disconnect ausschalten.
            Hooks.resetOnErrorDropped();
            Hooks.onErrorDropped(th -> {
            });
        }
    }

    // private void startServerSpringRest() {
    //  new SpringApplicationBuilder(JsyncServerApplication.class)
    //          //.properties("server.port=8081") // Does not work, if 'server.port' exist in application.yml.
    //          //.run(args);
    //          .run(new String[]{"--server.port=8001"});
    //
    // if (!CLOSEABLES.containsKey("springrest")) {
    // JSyncRestApplication server = new JSyncRestApplication();
    // server.start(new String[] {
    // "--server.port=8003"
    // });
    // CLOSEABLES.put("springrest", () -> server.stop());
    // }
    // }

    // private void startServerSpringWebflux() {
    // if (!CLOSEABLES.containsKey("springwebflux")) {
    // JSyncWebfluxApplication server = new JSyncWebfluxApplication();
    // server.start(new String[] {
    // "--server.port=8004"
    // });
    // CLOSEABLES.put("springwebflux", () -> server.stop());
    // }
    // }

    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri) {
        final Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        final List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, PathFilterNoOp.INSTANCE, syncItem -> {
            syncItemsSender.add(syncItem);
            final String checksum = client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
                // System.out.println("Sender Bytes read: " + i);
            });
            syncItem.setChecksum(checksum);
        });

        final List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, PathFilterNoOp.INSTANCE, syncItem -> {
            syncItemsReceiver.add(syncItem);
            final String checksum = client.generateChecksum(EFileSystem.RECEIVER, syncItem, i -> {
                // System.out.println("Sender Bytes read: " + i);
            });
            syncItem.setChecksum(checksum);
        });

        final List<SyncPair> syncPairs = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncPairs.forEach(SyncPair::validateStatus);

        client.syncReceiver(syncPairs, new TestClientListener());

        client.disconnectFileSystems();
    }

    // @Test
    // void testSpringRest() {
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_SOURCE.toString());
    // URI receiverUri = new URI("jsync://localhost:8003/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);
    //
    // assertTrue(true);
    // }

    // @Test
    // void testSpringWebFlux() {
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_SOURCE.toString());
    // URI receiverUri = new URI("jsync://localhost:8003/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_WEB_CLIENT);
    //
    // assertTrue(true);
    // }
}
