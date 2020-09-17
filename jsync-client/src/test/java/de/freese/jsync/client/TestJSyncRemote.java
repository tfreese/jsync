/**
 * Created: 22.10.2016
 */

package de.freese.jsync.client;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import de.freese.jsync.nio.server.JSyncServer;
import de.freese.jsync.nio.server.handler.JSyncIoHandler;
import de.freese.jsync.spring.server.JsyncServerApplication;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
// @Disabled
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
    private static final List<AutoCloseable> CLOSEABLES = new ArrayList<>();

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
        for (AutoCloseable closeable : CLOSEABLES)
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
        options = new Builder().delete(true).dryRun(false).followSymLinks(false).checksum(true).build();

        // NIO-Server Sender
        JSyncServer serverNioSender = new JSyncServer(8001, 2, 4);
        serverNioSender.setName("sender");
        serverNioSender.setIoHandler(new JSyncIoHandler());
        serverNioSender.start();
        CLOSEABLES.add(() -> serverNioSender.stop());

        // NIO-Server Receiver
        JSyncServer serverNioReceiver = new JSyncServer(8002, 2, 4);
        serverNioReceiver.setName("receiver");
        serverNioReceiver.setIoHandler(new JSyncIoHandler());
        serverNioReceiver.start();
        CLOSEABLES.add(() -> serverNioReceiver.stop());

        // Spring-Server Sender
        JsyncServerApplication serverSpringSender = new JsyncServerApplication();
        serverSpringSender.start(new String[]
        {
                "--server.port=8003"
        });
        CLOSEABLES.add(() -> serverSpringSender.stop());

        // Spring-Server Receiver
        JsyncServerApplication serverSpringReceiver = new JsyncServerApplication();
        serverSpringReceiver.start(new String[]
        {
                "--server.port=8004"
        });
        CLOSEABLES.add(() -> serverSpringReceiver.stop());
    }

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
            client.generateChecksum(EFileSystem.SENDER, syncItem, null);
        });

        List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, syncItem -> {
            syncItemsReceiver.add(syncItem);
            client.generateChecksum(EFileSystem.RECEIVER, syncItem, null);
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

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioAsyncLocalToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_ASYNC);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioAsyncRemoteToLocal() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_ASYNC);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioAsyncRemoteToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_ASYNC);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioBlockingLocalToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_BLOCKING);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioBlockingRemoteToLocal() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_BLOCKING);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testNioBlockingRemoteToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.NIO_BLOCKING);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    // @Disabled
    void testSpringRestTemplateLocalToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = new URI("jsync://localhost:8004/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testSpringRestTemplateRemoteToLocal() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

//        // @formatter:off
//        new SpringApplicationBuilder(JsyncServerApplication.class)
//                //.properties("server.port=8081") // Funktioniert nicht, wenn server.port in application.yml enthalten ist.
//                //.run(args);
//                .run(new String[]{"--server.port=8001"});
//        // @formatter:on

        URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    @Disabled
    void testSpringRestTemplateRemoteToRemote() throws Exception
    {
        System.out.println();
        TimeUnit.MILLISECONDS.sleep(500);

        URI senderUri = new URI("jsync://localhost:8003/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8004/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, RemoteMode.SPRING_REST_TEMPLATE);

        assertTrue(true);
    }
}
