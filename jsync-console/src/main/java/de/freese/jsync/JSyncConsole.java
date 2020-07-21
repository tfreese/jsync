/**
 * Created: 23.10.2016
 */
package de.freese.jsync;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.arguments.ArgumentParser;
import de.freese.jsync.arguments.ArgumentParserApacheCommonsCli;
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
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.utils.JSyncUtils;

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

        // args2 = new String[]
        // {
        // "--delete", "--follow-symlinks", "--checksum", "-s", "jsync://localhost:8001/PATH/DIR", "-d", "jsync://localhost:8002/PATH/DIR"
        // };

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
        int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

        // @formatter:off
        Options options = new Builder()
                .delete(argumentParser.delete())
                .followSymLinks(argumentParser.followSymlinks())
                .dryRun(argumentParser.dryRun())
                .checksum(argumentParser.checksum())
                .executorService(Executors.newFixedThreadPool(poolSize))
                .build()
                ;
        // @formatter:on

        Sender sender = null;

        if (argumentParser.sender().startsWith("jsync"))
        {
            // Remote
            sender = new RemoteSenderBlocking(new URI(argumentParser.sender()));
        }
        else
        {
            sender = new LocalhostSender(new File(argumentParser.sender()).toURI());
        }

        Receiver receiver = null;

        if (argumentParser.receiver().startsWith("jsync"))
        {
            // Remote
            receiver = new RemoteReceiverBlocking(new URI(argumentParser.receiver()));
        }
        else
        {
            receiver = new LocalhostReceiver(new File(argumentParser.receiver()).toURI());
        }

        try
        {
            syncDirectories(options, sender, receiver, new ConsoleClientListener());
        }
        finally
        {
            JSyncUtils.shutdown(options.getExecutorService(), LoggerFactory.getLogger(JSyncConsole.class));
        }
    }

    /**
     * @param options {@link Options} options
     * @param sender {@link Sender}
     * @param receiver {@link Receiver}
     * @param clientListener {@link ClientListener}
     * @throws Exception Falls was schief geht.
     */
    public void syncDirectories(final Options options, final Sender sender, final Receiver receiver, final ClientListener clientListener) throws Exception
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
}
