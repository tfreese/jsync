// Created: 05.04.2018
package de.freese.jsync.client;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
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
    protected void copyFile(final Sender sender, final Receiver receiver, final SyncItem syncItem)
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
            // FileChannel.transferFrom(ReadableByteChannel, position, count);
            // FileChannel.transferTo(position, count, WritableByteChannel);

            ByteBuffer buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);

            @SuppressWarnings("unused")
            long bytesRead = 0;
            long bytesWrote = 0;

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
            receiver.validateFile(syncItem, getOptions().isChecksum());
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
        Predicate<SyncPair> isExisting = p -> p.getSource() != null;
        Predicate<SyncPair> isFile = p -> p.getSource().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
                .forEach(pair -> copyFile(sender, receiver,  pair.getSource()));
        //@formatter:on
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
        Predicate<SyncPair> isExisting = p -> p.getReceiver() != null;
        Predicate<SyncPair> isDirectory = p -> p.getReceiver().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isDirectory).and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteDirectory(receiver, pair.getReceiver().getRelativePath()));
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
        Predicate<SyncPair> isExisting = p -> p.getReceiver() != null;
        Predicate<SyncPair> isFile = p -> p.getReceiver().isFile();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteFile(receiver, pair.getReceiver().getRelativePath()));
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
        Predicate<SyncPair> isExisting = p -> p.getSource() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSource().isDirectory();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isDirectory).and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateDirectory(receiver, pair.getSource()));
        // @formatter:on
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Receiver}.<br>
     *
     * @param receiver {@link Receiver}
     * @param syncItem {@link SyncItem}
     */
    protected void updateDirectory(final Receiver receiver, final SyncItem syncItem)
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
     * @param syncItem {@link SyncItem}
     */
    protected void updateFile(final Receiver receiver, final SyncItem syncItem)
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
        Predicate<SyncPair> isExisting = p -> p.getSource() != null;
        Predicate<SyncPair> isFile = p -> p.getSource().isFile();
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isDifferentPermission.or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateFile(receiver, pair.getSource()));
        // @formatter:on
    }
}
