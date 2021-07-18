/**
 * Created: 23.10.2016
 */
package de.freese.jsync;

import java.net.URI;

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
import reactor.core.publisher.Flux;

/**
 * Consolen-Anwendung für jsync.<br>
 *
 * @author Thomas Freese
 */
public final class JSyncConsole
{
    /**
    *
    */
    public static final Logger LOGGER = LoggerFactory.getLogger(JSyncConsole.class);

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        String[] args2 = args;

        if (args2.length == 0)
        {
            args2 = new String[]
            {
                    "--delete", "--follow-symlinks", "--checksum", "-s", "file:///home/tommy/git/jsync/jsync-console", "-r", "file:///tmp/jsync-console"
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

    // /**
    // *
    // */
    // private static void disableLogging()
    // {
    // // ch.qos.logback.classic.Logger Logger rootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    // // rootLogger.setLevel(Level.OFF);
    // //
    // // LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    // // Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    // // rootLogger.setLevel(Level.INFO);
    // }

    /**
     * @param argumentParser {@link ArgumentParser}
     *
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

        System.out.println("Start syncronisation");
        syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener());
        System.out.println("Syncronisation finished");
    }

    /**
     * @param options {@link Options} options
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param clientListener {@link ClientListener}
     *
     * @throws Exception Falls was schief geht.
     */
    public void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener) throws Exception
    {
        Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        Flux<SyncItem> syncItemsSender = client.generateSyncItems(EFileSystem.SENDER, i -> {
        });
        Flux<SyncItem> syncItemsReceiver = client.generateSyncItems(EFileSystem.RECEIVER, i -> {
        });

        Flux<SyncPair> syncList = client.mergeSyncItems(syncItemsSender, syncItemsReceiver);

        syncList.subscribe(SyncPair::validateStatus);

        client.syncReceiver(syncList, clientListener);

        client.disconnectFileSystems();
    }
}
