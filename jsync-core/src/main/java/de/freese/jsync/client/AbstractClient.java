// Created: 05.04.2018
package de.freese.jsync.client;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;

/**
 * Basis-Implementierung des {@link Client}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractClient implements Client
{
    /**
     *
     */
    private final ClientListener clientListener;

    /**
    *
    */
    private final Options options;

    /**
     * Erzeugt eine neue Instanz von {@link AbstractClient}.
     *
     * @param options {@link Options}
     * @param clientListener {@link ClientListener}
     */
    public AbstractClient(final Options options, final ClientListener clientListener)
    {
        super();

        this.options = Objects.requireNonNull(options, "options required");
        this.clientListener = Objects.requireNonNull(clientListener, "clientListener required");
    }

    /**
     * Kopieren der Dateien von der Quelle in die Senke<br>
     *
     * @param sender {@link Sender}
     * @param receiver {@link Receiver}
     * @param syncItem {@link SyncItem}
     */
    protected void copyFile(final Sender sender, final Receiver receiver, final FileSyncItem syncItem)
    {
        long fileSize = syncItem.getSize();
        getClientListener().copyFileProgress(syncItem, fileSize, 0);

        if (getOptions().isDryRun())
        {
            getClientListener().copyFileProgress(syncItem, fileSize, fileSize);
            return;
        }

        try (ReadableByteChannel readableByteChannel = sender.getChannel(syncItem);
             WritableByteChannel writableByteChannel = receiver.getChannel(syncItem))
        {
            // readableByteChannel = new MonitoringReadableByteChannel(readableByteChannel, monitorRead, fileSize);
            // writableByteChannel = new MonitoringWritableByteChannel(writableByteChannel, monitorWrite, fileSize);

            ByteBuffer buffer = ByteBuffer.allocateDirect(getOptions().getBufferSize());

            @SuppressWarnings("unused")
            long bytesRead = 0;
            long bytesWrote = 0;

            // while ((readableByteChannel.read(buffer) != -1) || (bytesWrote < fileSize))
            while (bytesWrote < fileSize)
            {
                bytesRead += readableByteChannel.read(buffer);
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    bytesWrote += writableByteChannel.write(buffer);
                    getClientListener().copyFileProgress(syncItem, fileSize, bytesWrote);
                }

                buffer.clear();
            }
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }

        try
        {
            // Datei überprüfen.
            getClientListener().validateFile(getOptions(), syncItem);
            receiver.validateFile(syncItem);

            // Attribute aktualisieren.
            getClientListener().updateFile(getOptions(), syncItem);
            receiver.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Kopieren der Dateien auf den {@link Receiver}<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_SIZE}<br>
     * {@link SyncStatus#DIFFERENT_CHECKSUM}<br>
     *
     * @param sender Sender
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void copyFiles(final Sender sender, final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
                .forEach(pair -> copyFile(sender, receiver, (FileSyncItem) pair.getSource()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Receiver}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void createDirectories(final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getSource() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInSource))
                .forEach(pair -> createDirectory(receiver, pair.getSource().getRelativePath()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Receiver}.<br>
     *
     * @param receiver {@link Receiver}
     * @param directory String
     */
    protected void createDirectory(final Receiver receiver, final String directory)
    {
        getClientListener().createDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            receiver.createDirectory(directory);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Löschen der Verzeichnisse und Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void deleteDirectories(final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getTarget() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteDirectory(receiver, pair.getTarget().getRelativePath()));
        // @formatter:on
    }

    /**
     * Löscht ein Verzeichnismit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param receiver {@link Receiver}
     * @param directory String
     */
    protected void deleteDirectory(final Receiver receiver, final String directory)
    {
        getClientListener().deleteDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            receiver.deleteDirectory(directory);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Löscht eine Datei mit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param receiver {@link Receiver}
     * @param file String
     */
    protected void deleteFile(final Receiver receiver, final String file)
    {
        getClientListener().deleteFile(getOptions(), file);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            receiver.deleteFile(file);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Löschen der Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void deleteFiles(final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getTarget() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteFile(receiver, pair.getTarget().getRelativePath()));
        // @formatter:on
    }

    /**
     * @return {@link ClientListener}
     */
    protected ClientListener getClientListener()
    {
        return this.clientListener;
    }

    /**
     * @return {@link Options}
     */
    protected Options getOptions()
    {
        return this.options;
    }

    /**
     * Vereinigt die Ergebnisse vom {@link Sender} und vom {@link Receiver}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param syncItemsSender {@link List}
     * @param syncItemsReceiver {@link List}
     * @return {@link List}
     */
    protected List<SyncPair> mergeSyncItems(final List<SyncItem> syncItemsSender, final List<SyncItem> syncItemsReceiver)
    {
        // Map der ReceiverItems bauen.
        Map<String, SyncItem> mapReceiver = syncItemsReceiver.stream().collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity()));

        // @formatter:off
        List<SyncPair> fileList = syncItemsSender.stream()
                .map(senderItem -> new SyncPair(senderItem, mapReceiver.remove(senderItem.getRelativePath())))
                .collect(Collectors.toList());
        // @formatter:on

        // Was jetzt noch in der Receiver-Map drin ist, muss gelöscht werden (source = null).
        mapReceiver.forEach((key, value) -> fileList.add(new SyncPair(null, value)));

        // SyncStatus ermitteln.
        // @formatter:off
        fileList.stream()
                .peek(SyncPair::validateStatus)
                .forEach(getClientListener()::debugSyncPair);
        // @formatter:on

        return fileList;
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Receiver}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void updateDirectories(final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getSource() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateDirectory(receiver, (DirectorySyncItem) pair.getSource()));
        // @formatter:on
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Receiver}.<br>
     *
     * @param receiver {@link Receiver}
     * @param syncItem {@link DirectorySyncItem}
     */
    protected void updateDirectory(final Receiver receiver, final DirectorySyncItem syncItem)
    {
        getClientListener().updateDirectory(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            receiver.updateDirectory(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Receiver}.<br>
     *
     * @param receiver {@link Receiver}
     * @param syncItem {@link FileSyncItem}
     */
    protected void updateFile(final Receiver receiver, final FileSyncItem syncItem)
    {
        getClientListener().updateFile(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            receiver.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Receiver}.<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param receiver {@link Receiver}
     * @param syncList {@link List}
     */
    protected void updateFiles(final Receiver receiver, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isDifferentPermission.or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateFile(receiver, (FileSyncItem) pair.getSource()));
        // @formatter:on
    }
}
