// Created: 05.04.2018
package de.freese.jsync.client;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;

/**
 * Default-Implementierung des {@link Client}.
 *
 * @author Thomas Freese
 */
public class DefaultClient extends AbstractClient
{
    /**
     * Erzeugt eine neue Instanz von {@link DefaultClient}.
     *
     * @param options {@link Options}
     * @param clientListener {@link ClientListener}
     */
    public DefaultClient(final Options options, final ClientListener clientListener)
    {
        super(options, clientListener);
    }

    /**
     * @see de.freese.jsync.client.Client#createSyncList(de.freese.jsync.filesystem.sender.Sender, de.freese.jsync.generator.listener.GeneratorListener,
     *      de.freese.jsync.filesystem.receiver.Receiver, de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public List<SyncPair> createSyncList(final Sender sender, final GeneratorListener senderListener, final Receiver receiver,
                                         final GeneratorListener receiverListener)
        throws Exception
    {
        getClientListener().dryRunInfo(getOptions());
        getClientListener().generatingFileListInfo();

        Map<String, SyncItem> fileMapSender = sender.createSyncItems(senderListener);
        Map<String, SyncItem> fileMapReceiver = receiver.createSyncItems(receiverListener);

        // FutureTask<Map<String, SyncItem>> futureTask = new FutureTask<>(callable);
        // futureTask.run(); // Synchron laufen Lassen.
        // CompletableFuture.runAsync(futureTask);
        // Future<Map<String, SyncItem>> futureTask = getOptions().getExecutorService().submit(callable);

        // Listen mergen.
        List<SyncPair> syncList = mergeSyncItems(fileMapSender, fileMapReceiver);

        return syncList;
    }

    /**
     * @see de.freese.jsync.client.Client#syncReceiver(de.freese.jsync.filesystem.sender.Sender, de.freese.jsync.filesystem.receiver.Receiver, java.util.List)
     */
    @Override
    public void syncReceiver(final Sender sender, final Receiver receiver, final List<SyncPair> syncList) throws Exception
    {
        getClientListener().syncStartInfo();

        // Alles rausfiltern was bereits synchronized ist.
        Predicate<SyncPair> isSynchronised = p -> SyncStatus.SYNCHRONIZED.equals(p.getStatus());
        List<SyncPair> list = syncList.stream().filter(isSynchronised.negate()).collect(Collectors.toList());

        // Löschen
        if (getOptions().isDelete())
        {
            deleteFiles(receiver, list);
            deleteDirectories(receiver, list);
        }

        // Neue Verzeichnisse erstellen.
        createDirectories(receiver, list);

        // Neue oder geänderte Dateien kopieren.
        copyFiles(sender, receiver, list);

        // Aktualisieren von Datei-Attributen.
        updateFiles(receiver, list);

        // Aktualisieren von Verzeichniss-Attributen.
        updateDirectories(receiver, list);

        getClientListener().syncFinishedInfo();
    }
}
