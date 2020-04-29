/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

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
public class TestJSync extends AbstractJSyncTest
{
    /**
     * Erstellt ein neues {@link TestJSync} Object.
     */
    public TestJSync()
    {
        super();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test010RemoteSourceRemoteTarget() throws Exception
    {
        System.out.println();

        IoHandler jsyncIoHandler = new JSyncIoHandler();

        JSyncServer serverSource = new JSyncServer(8001, 1);
        serverSource.setIoHandler(jsyncIoHandler);
        serverSource.start();

        JSyncServer serverTarget = new JSyncServer(8002, 1);
        serverTarget.setIoHandler(jsyncIoHandler);
        serverTarget.start();

        Options options = new Options();
        options.setDelete(true);
        options.setDryRun(false);
        options.setFollowSymLinks(false);
        options.setChecksum(true);
        // options.setBufferSize(2);

        // URI source = new URI("jsync", null, "localhost", 8001, "/" + PATH_QUELLE.toString(), null, null);
        // URI target = new URI("jsync", null, "localhost", 8002, "/" + PATH_ZIEL.toString(), null, null);
        URI source = new URI("jsync://localhost:8001/" + PATH_QUELLE.toString());
        URI target = new URI("jsync://localhost:8002/" + PATH_ZIEL.toString());

        JSync jSync = new JSync();
        jSync.syncDirectories(options, source, target, new ConsoleClientListener(), new ConsoleGeneratorListener("Source"),
                new ConsoleGeneratorListener("Target"));

        serverSource.stop();
        serverTarget.stop();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test020LocalSourceLocalTarget() throws Exception
    {
        System.out.println();

        Options options = new Options();
        options.setDelete(true);
        options.setDryRun(false);
        options.setFollowSymLinks(true);
        options.setChecksum(true);
        // options.setBufferSize(2);

        URI source = PATH_QUELLE.toUri();
        URI target = PATH_ZIEL.toUri();

        JSync jSync = new JSync();
        jSync.syncDirectories(options, source, target, new ConsoleClientListener(), new ConsoleGeneratorListener("Source"),
                new ConsoleGeneratorListener("Target"));
    }
}
