// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.FileSystemFactory;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.utils.JSyncUtils;
import reactor.core.publisher.Flux;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    private final String receiverPath;

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
    private final String senderPath;

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
    protected AbstractClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        this.options = Objects.requireNonNull(options, "options required");
        this.senderUri = Objects.requireNonNull(senderUri, "senderUri required");
        this.receiverUri = Objects.requireNonNull(receiverUri, "receiverUri required");

        this.senderPath = JSyncUtils.normalizePath(senderUri);
        this.receiverPath = JSyncUtils.normalizePath(receiverUri);

        this.sender = FileSystemFactory.getInstance().createSender(senderUri);
        this.receiver = FileSystemFactory.getInstance().createReceiver(receiverUri);
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

        long sizeOfFile = syncItem.getSize();

        try
        {
            Flux<ByteBuffer> fileList = getSender().readFile(getSenderPath(), syncItem.getRelativePath(), sizeOfFile);

            AtomicLong bytesTransferred = new AtomicLong(0);

            getReceiver().writeFile(getReceiverPath(), syncItem.getRelativePath(), sizeOfFile, fileList).doOnNext(bytesWritten -> {
                getLogger().debug("CHUNK_COMPLETED: bytesWritten = {}", bytesWritten);

                long writtenBytesSum = bytesTransferred.addAndGet(bytesWritten);
                clientListener.copyProgress(getOptions(), syncItem, writtenBytesSum);
            }).blockLast();
        }
        catch (Exception ex)
        {
            clientListener.error(null, ex);

            return;
        }

        try
        {
            // Datei überprüfen.
            clientListener.validate(getOptions(), syncItem);
            getReceiver().validateFile(getReceiverPath(), syncItem, getOptions().isChecksum(),
                    bytesRead -> clientListener.checksumProgress(getOptions(), syncItem, bytesRead));
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
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void copyFiles(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                        .or(isDifferentSize)
                        .or(isDifferentChecksum)
                )
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> copyFile(pair.getSenderItem(), clientListener))
            ;
        //@formatter:on
    }

    /**
     * Erstellen von leeren Verzeichnissen mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void createDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isEmpty = p -> p.getSenderItem().getSize() == 0;

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInTarget)
                .and(isEmpty)
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> createDirectory(pair.getSenderItem(), clientListener))
            ;
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
            getReceiver().createDirectory(getReceiverPath(), syncItem.getRelativePath());
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
            getReceiver().delete(getReceiverPath(), syncItem.getRelativePath(), getOptions().isFollowSymLinks());
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
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getReceiverItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInTarget)
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> delete(pair.getReceiverItem(), clientListener))
            ;
        // @formatter:on
    }

    /**
     * Löschen der Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteFiles(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isFile = p -> p.getReceiverItem().isFile();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInTarget)
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> delete(pair.getReceiverItem(), clientListener))
            ;
        // @formatter:on
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
    public String generateChecksum(final EFileSystem fileSystem, final SyncItem syncItem, final LongConsumer consumerChecksumBytesRead)
    {
        if (!getOptions().isChecksum() || !syncItem.isFile())
        {
            return null;
        }

        FileSystem fs = null;
        String baseDir = null;

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            fs = getSender();
            baseDir = getSenderPath();
        }
        else
        {
            fs = getReceiver();
            baseDir = getReceiverPath();
        }

        return fs.generateChecksum(baseDir, syncItem.getRelativePath(), consumerChecksumBytesRead);
    }

    /**
     * @see de.freese.jsync.client.Client#generateSyncItems(de.freese.jsync.filesystem.EFileSystem, de.freese.jsync.filter.PathFilter)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final EFileSystem fileSystem, final PathFilter pathFilter)
    {
        FileSystem fs = null;
        String baseDir = null;

        if (EFileSystem.SENDER.equals(fileSystem))
        {
            fs = getSender();
            baseDir = getSenderPath();
        }
        else
        {
            fs = getReceiver();
            baseDir = getReceiverPath();
        }

        // @formatter:off
        return fs.generateSyncItems(baseDir, getOptions().isFollowSymLinks(), pathFilter)
                .doOnError(ex -> getLogger().error(null, ex))
                ;
        // @formatter:on
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
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
     * @return String
     */
    protected String getReceiverPath()
    {
        return this.receiverPath;
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
     * @return String
     */
    protected String getSenderPath()
    {
        return this.senderPath;
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
            getReceiver().update(getReceiverPath(), syncItem);
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
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void updateDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInSource
                        .or(isDifferentPermission)
                        .or(isDifferentTimestamp)
                        .or(isDifferentUser)
                        .or(isDifferentGroup)
                )
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> update(pair.getSenderItem(), clientListener))
            ;
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
     * @param syncPairs {@link List}
     * @param clientListener {@link ClientListener}
     */
    protected void updateFiles(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInSource
                        .or(isDifferentPermission)
                        .or(isDifferentTimestamp)
                        .or(isDifferentUser)
                        .or(isDifferentGroup)
                )
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> update(pair.getSenderItem(), clientListener))
            ;
        // @formatter:on
    }
}
