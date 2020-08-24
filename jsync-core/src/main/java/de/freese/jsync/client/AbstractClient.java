// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.receiver.RemoteReceiverBlocking;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.RemoteSenderAsync;
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
    private final Options options;

    /**
     *
     */
    private final Receiver receiver;

    /**
     *
     */
    private final URI receiverUri;

    /**
     *
     */
    private final Sender sender;

    /**
     *
     */
    private final URI senderUri;

    /**
     * Erzeugt eine neue Instanz von {@link AbstractClient}.
     *
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     */
    public AbstractClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        super();

        this.options = Objects.requireNonNull(options, "options required");
        this.senderUri = Objects.requireNonNull(senderUri, "senderUri required");
        this.receiverUri = Objects.requireNonNull(receiverUri, "receiverUri required");

        if ((senderUri.getScheme() != null) && senderUri.getScheme().startsWith("jsync"))
        {
            // this.sender = new RemoteSenderBlocking();
            this.sender = new RemoteSenderAsync();
        }
        else
        {
            this.sender = new LocalhostSender();
        }

        if ((receiverUri.getScheme() != null) && receiverUri.getScheme().startsWith("jsync"))
        {
            this.receiver = new RemoteReceiverBlocking();
        }
        else
        {
            this.receiver = new LocalhostReceiver();
        }
    }

    /**
     * @see de.freese.jsync.client.Client#connectFileSystems()
     */
    @Override
    public void connectFileSystems()
    {
        getSender().connect(getSenderUri());
        getReceiver().connect(getReceiverUri());
    }

    /**
     * @see de.freese.jsync.client.Client#disconnectFileSystems()
     */
    @Override
    public void disconnectFileSystems()
    {
        getSender().disconnect();
        getReceiver().disconnect();
    }

    /**
     * @see de.freese.jsync.client.Client#generateChecksum(de.freese.jsync.filesystem.EFileSystem, de.freese.jsync.model.SyncItem,
     *      java.util.function.LongConsumer)
     */
    @Override
    public void generateChecksum(final EFileSystem fileSystem, final SyncItem syncItem, final LongConsumer consumerBytesRead)
    {
        if (!getOptions().isChecksum() || !syncItem.isFile())
        {
            return;
        }

        FileSystem fs = null;
        URI uri = null;

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            fs = getSender();
            uri = getSenderUri();
        }
        else
        {
            fs = getReceiver();
            uri = getReceiverUri();
        }

        String checksum = fs.getChecksum(uri.getPath(), syncItem.getRelativePath(), consumerBytesRead);
        syncItem.setChecksum(checksum);
    }

    /**
     * @see de.freese.jsync.client.Client#generateSyncItems(de.freese.jsync.filesystem.EFileSystem, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final EFileSystem fileSystem, final Consumer<SyncItem> consumerSyncItem)
    {
        FileSystem fs = null;
        URI uri = null;

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            fs = getSender();
            uri = getSenderUri();
        }
        else
        {
            fs = getReceiver();
            uri = getReceiverUri();
        }

        fs.generateSyncItems(uri.getPath(), getOptions().isFollowSymLinks(), consumerSyncItem);
    }

    /**
     * Kopieren der Dateien von der Quelle in die Senke<br>
     *
     * @param syncItem {@link SyncItem}
     * @param clientListener {@link ClientListener}
     */
    protected void copyFile(final SyncItem syncItem, final ClientListener clientListener)
    {
        clientListener.copyProgress(getOptions(), syncItem, 0);

        if (getOptions().isDryRun())
        {
            clientListener.copyProgress(getOptions(), syncItem, syncItem.getSize());
            return;
        }

        try (ReadableByteChannel readableByteChannel = getSender().getChannel(getSenderUri().getPath(), syncItem.getRelativePath());
             WritableByteChannel writableByteChannel = getReceiver().getChannel(getReceiverUri().getPath(), syncItem.getRelativePath()))
        {
            // readableByteChannel = new MonitoringReadableByteChannel(readableByteChannel, monitorRead, fileSize);
            // writableByteChannel = new MonitoringWritableByteChannel(writableByteChannel, monitorWrite, fileSize);
            // FileChannel.transferFrom(ReadableByteChannel, position, count);
            // FileChannel.transferTo(position, count, WritableByteChannel);

            ByteBuffer buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);

            @SuppressWarnings("unused")
            long bytesRead = 0;
            long bytesWrote = 0;

            while (bytesWrote < syncItem.getSize())
            {
                bytesRead += readableByteChannel.read(buffer);
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    bytesWrote += writableByteChannel.write(buffer);
                    clientListener.copyProgress(getOptions(), syncItem, bytesWrote);
                }

                buffer.clear();
            }
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);
        }

        try
        {
            // Datei überprüfen.
            clientListener.validate(getOptions(), syncItem);
            getReceiver().validateFile(getReceiverUri().getPath(), syncItem, getOptions().isChecksum());
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);
        }
    }

    /**
     * Kopieren der Dateien auf den {@link Receiver}<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_SIZE}<br>
     * {@link SyncStatus#DIFFERENT_CHECKSUM}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void copyFiles(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
                .forEach(pair -> copyFile(pair.getSenderItem(), clientListener));
        //@formatter:on
    }

    /**
     * Erstellen von leeren Verzeichnissen mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void createDirectories(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isEmpty = p -> p.getSenderItem().getSize() == 0;

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isDirectory).and(isOnlyInTarget).and(isEmpty))
                .forEach(pair -> createDirectory(pair.getSenderItem(), clientListener));
        // @formatter:on
    }

    /**
     * Erstellt ein Verzeichnis auf dem {@link Receiver}.<br>
     *
     * @param syncItem {@link SyncItem}
     * @param clientListener {@link ClientListener}
     */
    protected void createDirectory(final SyncItem syncItem, final ClientListener clientListener)
    {
        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            getReceiver().createDirectory(getReceiverUri().getPath(), syncItem.getRelativePath());
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);
        }
    }

    /**
     * Löscht ein {@link SyncItem} mit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param syncItem {@link SyncItem}
     * @param clientListener {@link ClientListener}
     */
    protected void delete(final SyncItem syncItem, final ClientListener clientListener)
    {
        clientListener.delete(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            getReceiver().delete(getReceiverUri().getPath(), syncItem.getRelativePath(), getOptions().isFollowSymLinks());
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);
        }
    }

    /**
     * Löschen der Verzeichnisse und Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteDirectories(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getReceiverItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isDirectory).and(isOnlyInTarget))
                .forEach(pair -> delete(pair.getReceiverItem(), clientListener));
        // @formatter:on
    }

    /**
     * Löschen der Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteFiles(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isFile = p -> p.getReceiverItem().isFile();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isOnlyInTarget))
                .forEach(pair -> delete(pair.getReceiverItem(), clientListener));
        // @formatter:on
    }

    /**
     * @return {@link Options}
     */
    protected Options getOptions()
    {
        return this.options;
    }

    /**
     * @return {@link Receiver}
     */
    protected Receiver getReceiver()
    {
        return this.receiver;
    }

    /**
     * @return {@link URI}
     */
    protected URI getReceiverUri()
    {
        return this.receiverUri;
    }

    /**
     * @return {@link Sender}
     */
    protected Sender getSender()
    {
        return this.sender;
    }

    /**
     * @return {@link URI}
     */
    protected URI getSenderUri()
    {
        return this.senderUri;
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Receiver}.<br>
     *
     * @param syncItem {@link SyncItem}
     * @param clientListener {@link ClientListener}
     */
    protected void update(final SyncItem syncItem, final ClientListener clientListener)
    {
        clientListener.update(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            getReceiver().update(getReceiverUri().getPath(), syncItem);
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);
        }
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Receiver}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void updateDirectories(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isDirectory).and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> update(pair.getSenderItem(), clientListener));
        // @formatter:on
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Receiver}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param syncList {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void updateFiles(final List<SyncPair> syncList, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isExisting.and(isFile).and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> update(pair.getSenderItem(), clientListener));
        // @formatter:on
    }
}
