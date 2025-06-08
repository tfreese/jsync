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
import reactor.core.publisher.Flux;

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

/**
 * @author Thomas Freese
 */
public abstract class AbstractClient implements Client {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Options options;
    private final Receiver receiver;
    private final String receiverPath;
    private final URI receiverUri;
    private final Sender sender;
    private final String senderPath;
    private final URI senderUri;

    protected AbstractClient(final Options options, final URI senderUri, final URI receiverUri) {
        super();

        this.options = Objects.requireNonNull(options, "options required");
        this.senderUri = Objects.requireNonNull(senderUri, "senderUri required");
        this.receiverUri = Objects.requireNonNull(receiverUri, "receiverUri required");

        senderPath = JSyncUtils.normalizePath(senderUri);
        receiverPath = JSyncUtils.normalizePath(receiverUri);

        sender = FileSystemFactory.getInstance().createSender(senderUri);
        receiver = FileSystemFactory.getInstance().createReceiver(receiverUri);
    }

    @Override
    public void connectFileSystems() {
        getSender().connect(getSenderUri());
        getReceiver().connect(getReceiverUri());
    }

    @Override
    public void disconnectFileSystems() {
        getSender().disconnect();
        getReceiver().disconnect();
    }

    @Override
    public String generateChecksum(final EFileSystem fileSystem, final SyncItem syncItem, final LongConsumer consumerChecksumBytesRead) {
        if (!getOptions().isChecksum() || !syncItem.isFile()) {
            return null;
        }

        FileSystem fs = null;
        String baseDir = null;

        if (EFileSystem.SENDER.equals(fileSystem)) {
            fs = getSender();
            baseDir = getSenderPath();
        }
        else {
            fs = getReceiver();
            baseDir = getReceiverPath();
        }

        return fs.generateChecksum(baseDir, syncItem.getRelativePath(), consumerChecksumBytesRead);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final EFileSystem fileSystem, final PathFilter pathFilter) {
        FileSystem fs = null;
        String baseDir = null;

        if (EFileSystem.SENDER.equals(fileSystem)) {
            fs = getSender();
            baseDir = getSenderPath();
        }
        else {
            fs = getReceiver();
            baseDir = getReceiverPath();
        }

        return fs.generateSyncItems(baseDir, getOptions().isFollowSymLinks(), pathFilter)
                .doOnError(ex -> getLogger().error(ex.getMessage(), ex))
                ;
    }

    protected void copyFile(final SyncItem syncItem, final ClientListener clientListener) {
        clientListener.copyProgress(getOptions(), syncItem, 0);

        if (getOptions().isDryRun()) {
            clientListener.copyProgress(getOptions(), syncItem, syncItem.getSize());
            return;
        }

        final long sizeOfFile = syncItem.getSize();

        try {
            final Flux<ByteBuffer> fileList = getSender().readFile(getSenderPath(), syncItem.getRelativePath(), sizeOfFile);

            final AtomicLong bytesTransferred = new AtomicLong(0L);

            getReceiver().writeFile(getReceiverPath(), syncItem.getRelativePath(), sizeOfFile, fileList)
                    .doOnNext(bytesWritten -> {
                        getLogger().debug("CHUNK_COMPLETED: bytesWritten = {}", bytesWritten);

                        final long writtenBytesSum = bytesTransferred.addAndGet(bytesWritten);
                        clientListener.copyProgress(getOptions(), syncItem, writtenBytesSum);
                    }).blockLast();
        }
        catch (Exception ex) {
            clientListener.error(ex.getMessage(), ex);

            return;
        }

        try {
            // Datei überprüfen.
            clientListener.validate(getOptions(), syncItem);
            getReceiver().validateFile(getReceiverPath(), syncItem, getOptions().isChecksum(), bytesRead -> clientListener.checksumProgress(getOptions(), syncItem, bytesRead));
        }
        catch (Exception ex) {
            clientListener.error(ex.getMessage(), ex);
        }
    }

    protected void copyFiles(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        final Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        final Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        final Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        final Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        final Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        final Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                        .or(isDifferentSize)
                        .or(isDifferentChecksum)
                );

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> copyFile(pair.getSenderItem(), clientListener))
        ;
    }

    protected void createDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        final Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        final Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        final Predicate<SyncPair> isEmpty = p -> p.getSenderItem().getSize() == 0;

        final Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInTarget)
                .and(isEmpty);

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> createDirectory(pair.getSenderItem(), clientListener))
        ;
    }

    protected void createDirectory(final SyncItem syncItem, final ClientListener clientListener) {
        if (getOptions().isDryRun()) {
            return;
        }

        try {
            getReceiver().createDirectory(getReceiverPath(), syncItem.getRelativePath());
        }
        catch (Exception ex) {
            clientListener.error(ex.getMessage(), ex);
        }
    }

    protected void delete(final SyncItem syncItem, final ClientListener clientListener) {
        clientListener.delete(getOptions(), syncItem);

        if (getOptions().isDryRun()) {
            return;
        }

        try {
            getReceiver().delete(getReceiverPath(), syncItem.getRelativePath(), getOptions().isFollowSymLinks());
        }
        catch (Exception ex) {
            clientListener.error(ex.getMessage(), ex);
        }
    }

    protected void deleteDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        final Predicate<SyncPair> isDirectory = p -> p.getReceiverItem().isDirectory();
        final Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        final Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInTarget);

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> delete(pair.getReceiverItem(), clientListener))
        ;
    }

    protected void deleteFiles(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        final Predicate<SyncPair> isFile = p -> p.getReceiverItem().isFile();
        final Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        final Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInTarget);

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> delete(pair.getReceiverItem(), clientListener))
        ;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected Options getOptions() {
        return options;
    }

    protected Receiver getReceiver() {
        return receiver;
    }

    protected String getReceiverPath() {
        return receiverPath;
    }

    protected URI getReceiverUri() {
        return receiverUri;
    }

    protected Sender getSender() {
        return sender;
    }

    protected String getSenderPath() {
        return senderPath;
    }

    protected URI getSenderUri() {
        return senderUri;
    }

    protected void update(final SyncItem syncItem, final ClientListener clientListener) {
        clientListener.update(getOptions(), syncItem);

        if (getOptions().isDryRun()) {
            return;
        }

        try {
            getReceiver().update(getReceiverPath(), syncItem);
        }
        catch (Exception ex) {
            clientListener.error(ex.getMessage(), ex);
        }
    }

    protected void updateDirectories(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        final Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        final Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        final Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());

        final Predicate<SyncPair> filter = isExisting
                .and(isDirectory)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                );

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> update(pair.getSenderItem(), clientListener))
        ;
    }

    protected void updateFiles(final List<SyncPair> syncPairs, final ClientListener clientListener) {
        final Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        final Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        final Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        final Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());

        final Predicate<SyncPair> filter = isExisting
                .and(isFile)
                .and(isOnlyInSource
                        .or(isDifferentTimestamp)
                );

        syncPairs.stream()
                .filter(filter)
                .forEach(pair -> update(pair.getSenderItem(), clientListener))
        ;
    }
}
