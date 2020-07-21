/**
 * Created: 22.10.2016
 */

package de.freese.jsync.remote;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.AbstractJSyncTest;
import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.client.listener.ConsoleClientListener;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.receiver.RemoteReceiverBlocking;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.RemoteSenderBlocking;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.generator.listener.ConsoleGeneratorListener;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.server.JSyncServer;
import de.freese.jsync.server.handler.IoHandler;
import de.freese.jsync.server.handler.JSyncIoHandler;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestJSyncRemote extends AbstractJSyncTest
{
    /**
     * @param options {@link Options} options
     * @param sender {@link Sender}
     * @param receiver {@link Receiver}
     * @param clientListener {@link ClientListener}
     * @param senderListener {@link GeneratorListener}; optional.
     * @param receiverListener {@link GeneratorListener}; optional.
     * @throws Exception Falls was schief geht.
     */
    private void syncDirectories(final Options options, final Sender sender, final Receiver receiver, final ClientListener clientListener,
                                 final GeneratorListener senderListener, final GeneratorListener receiverListener)
        throws Exception
    {
        sender.connect();
        receiver.connect();

        List<SyncItem> itemsSender = sender.getSyncItems(options.isFollowSymLinks());
        List<SyncItem> itemsReceiver = receiver.getSyncItems(options.isFollowSymLinks());

        if (options.isChecksum())
        {
            for (SyncItem syncItem : itemsSender)
            {
                if (syncItem.isDirectory())
                {
                    continue;
                }

                String checksum = sender.getChecksum(syncItem.getRelativePath(), null);
                syncItem.setChecksum(checksum);
            }

            for (SyncItem syncItem : itemsReceiver)
            {
                if (syncItem.isDirectory())
                {
                    continue;
                }

                String checksum = receiver.getChecksum(syncItem.getRelativePath(), null);
                syncItem.setChecksum(checksum);
            }
        }

        Client client = new DefaultClient(options, clientListener);

        List<SyncPair> syncList = client.mergeSyncItems(itemsSender, itemsReceiver);

        client.syncReceiver(sender, receiver, syncList, options.isChecksum());

        sender.disconnect();
        receiver.disconnect();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test010RemoteSenderRemoteReceiver() throws Exception
    {
        System.out.println();

        IoHandler jsyncIoHandler = new JSyncIoHandler();

        JSyncServer serverSender = new JSyncServer(8001, 1);
        serverSender.setIoHandler(jsyncIoHandler);
        serverSender.start();

        JSyncServer serverReceiver = new JSyncServer(8002, 1);
        serverReceiver.setIoHandler(jsyncIoHandler);
        serverReceiver.start();

        Options options = new Builder().delete(true).dryRun(false).followSymLinks(false).checksum(true).build();

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI senderUri = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiverUri = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        syncDirectories(options, new RemoteSenderBlocking(senderUri), new RemoteReceiverBlocking(receiverUri), new ConsoleClientListener(),
                new ConsoleGeneratorListener("Sender"), new ConsoleGeneratorListener("Receiver"));

        serverSender.stop();
        serverReceiver.stop();

        assertTrue(true);
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test020LocalSenderLocalReceiver() throws Exception
    {
        System.out.println();

        Options options = new Builder().delete(true).dryRun(false).followSymLinks(true).checksum(true).build();

        URI senderUri = PATH_QUELLE.toUri();
        URI receiverUri = PATH_ZIEL.toUri();

        syncDirectories(options, new LocalhostSender(senderUri), new LocalhostReceiver(receiverUri), new ConsoleClientListener(),
                new ConsoleGeneratorListener("Sender"), new ConsoleGeneratorListener("Receiver"));

        assertTrue(true);
    }
}
