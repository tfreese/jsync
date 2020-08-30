/**
 * Created: 22.10.2016
 */

package de.freese.jsync.remote;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.freese.jsync.AbstractJSyncTest;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.client.listener.ConsoleClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.server.JSyncServer;
import de.freese.jsync.server.handler.JSyncIoHandler;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestJSyncRemote extends AbstractJSyncTest
{
    /**
     * Sync directories.
     *
     * @param options        {@link Options} options
     * @param senderUri      {@link URI}
     * @param receiverUri    {@link URI}
     * @param clientListener {@link ClientListener}
     *
     * @throws Exception Falls was schief geht.
     */
    private void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener) throws Exception
    {
        Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        List<SyncItem> syncItemsSender = new ArrayList<>();
        client.generateSyncItems(EFileSystem.SENDER, syncItem ->
        {
            syncItemsSender.add(syncItem);
            client.generateChecksum(EFileSystem.SENDER, syncItem, null);
        });

        List<SyncItem> syncItemsReceiver = new ArrayList<>();
        client.generateSyncItems(EFileSystem.RECEIVER, syncItem ->
        {
            syncItemsReceiver.add(syncItem);
            client.generateChecksum(EFileSystem.RECEIVER, syncItem, null);
        });

        List<SyncPair> syncList = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncList.stream().forEach(SyncPair::validateStatus);

        client.syncReceiver(syncList, clientListener);

        client.disconnectFileSystems();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test010RemoteToLocal() throws Exception
    {
        System.out.println();

        JSyncServer serverSender = new JSyncServer(8001, 3);
        serverSender.setIoHandler(new JSyncIoHandler());
        serverSender.start();

        Options options = new Builder().delete(true).dryRun(false).followSymLinks(false).checksum(true).build();

        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener());

        serverSender.stop();

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test090RemoteToRemote() throws Exception
    {
        System.out.println();

        JSyncServer serverSender = new JSyncServer(8001, 3);
        serverSender.setIoHandler(new JSyncIoHandler());
        serverSender.start();

        JSyncServer serverReceiver = new JSyncServer(8002, 3);
        serverReceiver.setIoHandler(new JSyncIoHandler());
        serverReceiver.start();

        Options options = new Builder().delete(true).dryRun(false).followSymLinks(false).checksum(true).build();

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener());

        serverSender.stop();
        serverReceiver.stop();

        assertTrue(true);
    }
}
