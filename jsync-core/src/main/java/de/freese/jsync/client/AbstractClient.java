// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public abstract class AbstractClient implements Client
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Options options;

    private final Receiver receiver;

    private final String receiverPath;

    private final URI receiverUri;

    private final Sender sender;

    private final String senderPath;

    private final URI senderUri;

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
     * java.util.function.LongConsumer)
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
                .doOnError(ex -> getLogger().error(ex.getMessage(), ex))
                ;
        // @formatter:on
    }

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

            getReceiver().writeFile(getReceiverPath(), syncItem.getRelativePath(), sizeOfFile, fileList).doOnNext(bytesWritten ->
            {
                getLogger().debug("CHUNK_COMPLETED: bytesWritten = {}", bytesWritten);

                long writtenBytesSum = bytesTransferred.addAndGet(bytesWritten);
                clientListener.copyProgress(getOptions(), syncItem, writtenBytesSum);
            }).blockLast();
        }
        catch (Exception ex)
        {
            clientListener.error(ex.getMessage(), ex);

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
            clientListener.error(ex.getMessage(), ex);
        }
    }

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
            clientListener.error(ex.getMessage(), ex);
        }
    }

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
            clientListener.error(ex.getMessage(), ex);
        }
    }

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

    protected Logger getLogger()
    {
        return this.logger;
    }

    protected Options getOptions()
    {
        return this.options;
    }

    protected Receiver getReceiver()
    {
        return this.receiver;
    }

    protected String getReceiverPath()
    {
        return this.receiverPath;
    }

    protected URI getReceiverUri()
    {
        return this.receiverUri;
    }

    protected Sender getSender()
    {
        return this.sender;
    }

    protected String getSenderPath()
    {
        return this.senderPath;
    }

    protected URI getSenderUri()
    {
        return this.senderUri;
    }

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
            clientListener.error(ex.getMessage(), ex);
        }
    }

    protected void updateDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                )
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> update(pair.getSenderItem(), clientListener))
            ;
        // @formatter:on
    }

    protected void updateFiles(final List<SyncPair> syncPairs, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());

        // @formatter:off
        Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                )
                ;

        syncPairs.stream()
            .filter(filter)
            .forEach(pair -> update(pair.getSenderItem(), clientListener))
            ;
        // @formatter:on
    }
}
