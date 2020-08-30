/**
 * Created: 23.10.2016
 */
package de.freese.jsync;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.arguments.ArgumentParser;
import de.freese.jsync.arguments.ArgumentParserApacheCommonsCli;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.client.listener.ConsoleClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * Consolen-Anwendung für jsync.<br>
 *
 * @author Thomas Freese
 */
public class JSyncConsole
{
    /**
    *
    */
    public static final Logger LOGGER = LoggerFactory.getLogger(JSyncConsole.class);

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        String[] args2 = args;

        if (args2.length == 0)
        {
            args2 = new String[]
            {
                    "--delete", "--follow-symlinks", "--checksum", "-s", "file:///home/tommy/git/jsync/jsync-console", "-r", "file:///tmp/jsync/target"
            };
            // args2 = new String[]
            // {
            // "--delete",
            // "--follow-symlinks",
            // "--checksum",
            // "-s",
            // "jsync://localhost:8001/home/tommy/git/jsync/jsync-console",
            // "-r",
            // "jsync://localhost:8002/tmp/jsync/target"
            // };
        }

        ArgumentParser argumentParser = null;

        try
        {
            argumentParser = new ArgumentParserApacheCommonsCli(args2);
            // argumentParser = new ArgumentParserJopt(args2);
        }
        catch (Exception ex)
        {
            LOGGER.error(null, ex);
        }

        if (!argumentParser.hasArgs())
        {
            argumentParser.printHelp(System.out);

            System.exit(0);
        }

        JSyncConsole jSync = new JSyncConsole();
        jSync.run(argumentParser);
    }

    /**
     * Erstellt ein neues {@link JSyncConsole} Object.
     */
    public JSyncConsole()
    {
        super();
    }

    /**
     * @param argumentParser {@link ArgumentParser}
     * @throws Exception Falls was schief geht.
     */
    public void run(final ArgumentParser argumentParser) throws Exception
    {
        // @formatter:off
        Options options = new Builder()
                .delete(argumentParser.delete())
                .followSymLinks(argumentParser.followSymlinks())
                .dryRun(argumentParser.dryRun())
                .checksum(argumentParser.checksum())
                .build()
                ;
        // @formatter:on

        URI senderUri = new URI(argumentParser.sender());
        URI receiverUri = new URI(argumentParser.receiver());

        syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener());
    }

    /**
     * @param options {@link Options} options
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param clientListener {@link ClientListener}
     * @throws Exception Falls was schief geht.
     */
    public void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener) throws Exception
    {
        Client client = new DefaultClient(options, senderUri, receiverUri);
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

        client.syncReceiver(syncList, clientListener);

        client.disconnectFileSystems();
    }
}
