// Created: 23.10.2016
package de.freese.jsync;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

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
 * @author Thomas Freese
 */
public final class JSyncConsole {
    public static final Logger LOGGER = LoggerFactory.getLogger(JSyncConsole.class);

    public static void main(final String[] args) throws Exception {
        String[] arguments = args;

        if (arguments.length == 0) {
            arguments = new String[]{"--delete", "--follow-symlinks", "--checksum", "-s", "file:///home/tommy/git/jsync/jsync-console", "-r", "file:///tmp/jsync-console"};
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

        try {
            argumentParser = new ArgumentParserApacheCommonsCli(arguments);
            // argumentParser = new ArgumentParserJopt(args2);
        }
        catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        if (!argumentParser.hasArgs()) {
            argumentParser.printHelp(System.out);

            System.exit(0);
        }

        final JSyncConsole jSync = new JSyncConsole();
        jSync.run(argumentParser);
    }

    // private static void disableLogging() {
    // // ch.qos.logback.classic.Logger Logger rootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    // // rootLogger.setLevel(Level.OFF);
    // //
    // // LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    // // Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    // // rootLogger.setLevel(Level.INFO);
    // }

    public void run(final ArgumentParser argumentParser) throws Exception {
        final Options options = new Builder()
                .delete(argumentParser.delete())
                .followSymLinks(argumentParser.followSymlinks())
                .dryRun(argumentParser.dryRun())
                .checksum(argumentParser.checksum())
                .build();

        final URI senderUri = new URI(argumentParser.sender());
        final URI receiverUri = new URI(argumentParser.receiver());

        LOGGER.info("Start synchronisation");
        syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener());
        LOGGER.info("Synchronisation finished");
    }

    public void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener) {
        final Client client = new DefaultClient(options, senderUri, receiverUri);
        client.connectFileSystems();

        final Flux<SyncItem> syncItemsSender = client.generateSyncItems(EFileSystem.SENDER, null)
                .doOnNext(syncItem -> {
                    final String checksum = client.generateChecksum(EFileSystem.SENDER, syncItem, i -> {
                        // System.out.println("Sender Bytes read: " + i);
                    });
                    syncItem.setChecksum(checksum);
                });

        final Flux<SyncItem> syncItemsReceiver = client.generateSyncItems(EFileSystem.RECEIVER, null)
                .doOnNext(syncItem -> {
                    final String checksum = client.generateChecksum(EFileSystem.RECEIVER, syncItem, i -> {
                        // System.out.println("Sender Bytes read: " + i);
                    });
                    syncItem.setChecksum(checksum);
                });

        final List<SyncPair> syncPairs = client.mergeSyncItems(syncItemsSender.collectList().block(), syncItemsReceiver.collectList().block());

        syncPairs.forEach(SyncPair::validateStatus);

        client.syncReceiver(syncPairs, clientListener);

        client.disconnectFileSystems();
    }
}
