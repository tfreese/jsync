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
import de.freese.jsync.arguments.ArgumentParser;
import de.freese.jsync.arguments.ArgumentParserApacheCommonsCli;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.client.listener.ConsoleClientListener;
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.filesystem.source.SourceFactory;
import de.freese.jsync.filesystem.target.Target;
import de.freese.jsync.filesystem.target.TargetFactory;
import de.freese.jsync.generator.listener.ConsoleGeneratorListener;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.util.JSyncUtils;

/**
 * Main-Class für jsync.<br>
 *
 * @author Thomas Freese
 */
public class JSync
{
    /**
    *
    */
    public static final Logger LOGGER = LoggerFactory.getLogger(JSync.class);

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

        JSync jSync = new JSync();
        jSync.run(argumentParser);
    }

    /**
     * Erstellt ein neues {@link JSync} Object.
     */
    public JSync()
    {
        super();
    }

    /**
     * @param argumentParser {@link ArgumentParser}
     * @throws Exception Falls was schief geht.
     */
    public void run(final ArgumentParser argumentParser) throws Exception
    {
        Options options = new Options();
        options.setDelete(argumentParser.delete());
        options.setFollowSymLinks(argumentParser.followSymlinks());
        options.setDryRun(argumentParser.dryRun());
        options.setChecksum(argumentParser.checksum());

        int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        options.setExecutorService(Executors.newFixedThreadPool(poolSize));

        String source = argumentParser.source();
        URI senderUri = null;

        if (!source.startsWith("jsync"))
        {
            // Kein Remote
            senderUri = new File(source).toURI();
        }
        else
        {
            senderUri = new URI(source);
        }

        String target = argumentParser.target();
        URI receiverUri = null;

        if (!target.startsWith("jsync"))
        {
            // Kein Remote
            receiverUri = new File(target).toURI();
        }
        else
        {
            receiverUri = new URI(target);
        }

        try
        {
            syncDirectories(options, senderUri, receiverUri, new ConsoleClientListener(), new ConsoleGeneratorListener("Sender"),
                    new ConsoleGeneratorListener("Receiver"));
        }
        finally
        {
            JSyncUtils.shutdown(options.getExecutorService(), LoggerFactory.getLogger(JSync.class));
        }
    }

    /**
     * @param options {@link Options} options
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param clientListener {@link ClientListener}
     * @param senderGeneratorListener {@link GeneratorListener}; optional.
     * @param receiverGeneratorListener {@link GeneratorListener}; optional.
     * @throws Exception Falls was schief geht.
     */
    public void syncDirectories(final Options options, final URI senderUri, final URI receiverUri, final ClientListener clientListener,
                                final GeneratorListener senderGeneratorListener, final GeneratorListener receiverGeneratorListener)
        throws Exception
    {
        Source source = SourceFactory.createSourceFromURI(options, senderUri);
        Target target = TargetFactory.createReceiverFromURI(options, receiverUri);

        source.connect();
        target.connect();

        Client client = new DefaultClient(options, clientListener);

        List<SyncPair> syncList = client.createSyncList(source, senderGeneratorListener, target, receiverGeneratorListener);

        client.syncReceiver(source, target, syncList);

        source.disconnect();
        target.disconnect();
    }
}
