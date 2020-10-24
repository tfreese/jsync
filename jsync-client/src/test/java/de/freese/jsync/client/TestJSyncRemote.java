/**
 * Created: 22.10.2016
 */

package de.freese.jsync.client;

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
import de.freese.jsync.AbstractJSyncTest;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.AbstractClient.RemoteMode;
import de.freese.jsync.client.listener.EmptyClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.nio.server.JSyncNioServer;
import de.freese.jsync.nio.server.handler.JSyncIoHandler;
import de.freese.jsync.rsocket.server.JsyncRSocketServer;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
// @Disabled("")
class TestJSyncRemote extends AbstractJSyncTest
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
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @BeforeAll
    static void beforeAll() throws Exception
    {
        options = new Builder().delete(true).checksum(true).followSymLinks(false).dryRun(false).build();
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
    private void startServerNio() throws Exception
    {
        if (!CLOSEABLES.containsKey("nio"))
        {
            JSyncNioServer server = new JSyncNioServer(8001, 2, 4);
            server.setName("nio");
            server.setIoHandler(new JSyncIoHandler());
            server.start();
            CLOSEABLES.put("nio", () -> server.stop());
        }
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    private void startServerRSocket() throws Exception
    {
        if (!CLOSEABLES.containsKey("rsocket"))
        {
            JsyncRSocketServer server = new JsyncRSocketServer();
            server.start(8002, 2, 4);
            CLOSEABLES.put("rsocket", () -> server.stop());
        }
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
     * Sync directories.
     *
     * @param options {@link Options} options
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param remoteMode {@link RemoteMode}
     * @throws Exception Falls was schief geht.
     */
    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final RemoteMode remoteMode) throws Exception
    {
        Client client = new DefaultClient(options, senderUri, receiverUri, remoteMode);
        client.connectFileSystems();

        List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, syncItem -> {
            syncItemsSender.add(syncItem);
            client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
            });
        });

        List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, syncItem -> {
            syncItemsReceiver.add(syncItem);
            client.generateChecksum(EFileSystem.RECEIVER, syncItem, i -> {
            });
        });

        List<SyncPair> syncList = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncList.stream().forEach(SyncPair::validateStatus);

        client.syncReceiver(syncList, new TestClientListener());

        client.disconnectFileSystems();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testLocalToLocal() throws Exception
    {
        System.out.println();

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, null);

        assertTrue(true);
    }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testNettyLocalToRemote() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerNetty();
    //
    // URI senderUri = PATH_QUELLE.toUri();
    // URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO);
    //
    // assertTrue(true);
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testNettyRemoteToLocal() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerNetty();
    //
    // URI senderUri = new URI("jsync://localhost:8002/" + PATH_QUELLE.toString());
    // URI receiverUri = PATH_ZIEL.toUri();
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO);
    //
    // assertTrue(true);
    // }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioRemoteToLocal() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerNio();

        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioRemoteToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerNio();

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8001, "/" + PATH_ZIEL.toString(), null, null);
        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8001/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testRSocketRemoteToLocal() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerRSocket();

        URI senderUri = new URI("jsync://localhost:8002/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, RemoteMode.RSOCKET);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testRSocketRemoteToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        startServerRSocket();

        URI senderUri = new URI("jsync://localhost:8002/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.RSOCKET);

        assertTrue(true);
    }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringRestLocalToRemote() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = PATH_QUELLE.toUri();
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
    // void testSpringRestRemoteToLocal() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
    // URI receiverUri = PATH_ZIEL.toUri();
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);
    //
    // assertTrue(true);
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringRestRemoteToRemote() throws Exception
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
    // void testSpringWebfluxLocalToRemote() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = PATH_QUELLE.toUri();
    // URI receiverUri = new URI("jsync://localhost:8003/" + PATH_ZIEL.toString());
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_WEB_CLIENT);
    //
    // assertTrue(true);
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringWebfluxRemoteToLocal() throws Exception
    // {
    // System.out.println();
    // TimeUnit.MILLISECONDS.sleep(500);
    //
    // startServerSpringRest();
    //
    // URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
    // URI receiverUri = PATH_ZIEL.toUri();
    //
    // syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_WEB_CLIENT);
    //
    // assertTrue(true);
    // }

    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @Test
    // void testSpringWebfluxRemoteToRemote() throws Exception
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
