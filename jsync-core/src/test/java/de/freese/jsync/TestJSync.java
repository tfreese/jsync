/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.client.listener.ConsoleClientListener;
import de.freese.jsync.generator.listener.ConsoleGeneratorListener;
import de.freese.jsync.server.JSyncServer;
import de.freese.jsync.server.handler.IoHandler;
import de.freese.jsync.server.handler.JSyncIoHandler;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestJSync extends AbstractJSyncTest
{
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

        Options options = new Options();
        options.setDelete(true);
        options.setDryRun(false);
        options.setFollowSymLinks(false);
        options.setChecksum(true);
        // options.setBufferSize(2);

        // URI sender = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI receiver = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI sender = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI receiver = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        JSync jSync = new JSync();
        jSync.syncDirectories(options, sender, receiver, new ConsoleClientListener(), new ConsoleGeneratorListener("Sender"),
                new ConsoleGeneratorListener("Receiver"));

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

        Options options = new Options();
        options.setDelete(true);
        options.setDryRun(false);
        options.setFollowSymLinks(true);
        options.setChecksum(true);
        // options.setBufferSize(2);

        URI sender = PATH_QUELLE.toUri();
        URI receiver = PATH_ZIEL.toUri();

        JSync jSync = new JSync();
        jSync.syncDirectories(options, sender, receiver, new ConsoleClientListener(), new ConsoleGeneratorListener("Sender"),
                new ConsoleGeneratorListener("Receiver"));

        assertTrue(true);
    }
}
