// Created: 05.04.2018
package de.freese.jsync.client;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.utils.JSyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Basis-Implementierung des {@link Client}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractClient implements Client
{
    /**
     * @author Thomas Freese
     */
    public enum RemoteMode
    {
        /**
         *
         */
        NIO,
        /**
        *
        */
        RSOCKET,
        /**
         *
         */
        SPRING_REST_TEMPLATE,
        /**
        *
        */
        SPRING_WEB_CLIENT;
    }

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
    private final RemoteMode remoteMode;

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
        this(options, senderUri, receiverUri, RemoteMode.NIO);
    }

    /**
     * Erzeugt eine neue Instanz von {@link AbstractClient}.
     *
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     * @param remoteMode {@link RemoteMode}
     */
    protected AbstractClient(final Options options, final URI senderUri, final URI receiverUri, final RemoteMode remoteMode)
    {
        this.options = Objects.requireNonNull(options, "options required");
        this.senderUri = Objects.requireNonNull(senderUri, "senderUri required");
        this.receiverUri = Objects.requireNonNull(receiverUri, "receiverUri required");
        this.remoteMode = remoteMode;

        this.senderPath = JSyncUtils.normalizePath(senderUri);
        this.receiverPath = JSyncUtils.normalizePath(receiverUri);

        // if ((senderUri.getScheme() != null) && senderUri.getScheme().startsWith("jsync"))
        // {
        // this.sender = switch (this.remoteMode)
        // {
        // case NIO -> new RemoteSenderNio();
        // case RSOCKET -> new RemoteSenderRSocket();
        // case SPRING_REST_TEMPLATE -> new RemoteSenderRestClient();
        // // case SPRING_WEB_CLIENT -> new RemoteSenderWebFluxClient();
        //
        // default -> throw new IllegalArgumentException("Unexpected remote mode: " + this.remoteMode);
        // };
        // }
        // else
        // {
        this.sender = new LocalhostSender();
        // }

        // if ((receiverUri.getScheme() != null) && receiverUri.getScheme().startsWith("jsync"))
        // {
        // this.receiver = switch (this.remoteMode)
        // {
        // case NIO -> new RemoteReceiverNio();
        // case RSOCKET -> new RemoteReceiverRSocket();
        // case SPRING_REST_TEMPLATE -> new RemoteReceiverRestClient();
        // // case SPRING_WEB_CLIENT -> new RemoteReceiverWebFluxClient();
        //
        // default -> throw new IllegalArgumentException("Unexpected remote mode: " + this.remoteMode);
        // };
        // }
        // else
        // {
        this.receiver = new LocalhostReceiver();
        // }
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
            Flux<ByteBuffer> fileFlux = getSender().readFile(getSenderPath(), syncItem.getRelativePath(), sizeOfFile);

            getReceiver().writeFile(this.senderPath, this.receiverPath, sizeOfFile, fileFlux);
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
            getReceiver().validateFile(getReceiverPath(), syncItem, getOptions().isChecksum());
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
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void copyFiles(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isFile).and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
            .subscribe(pair -> copyFile(pair.getSenderItem(), clientListener))
            ;
        //@formatter:on
    }

    /**
     * Erstellen von leeren Verzeichnissen mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void createDirectories(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isEmpty = p -> p.getSenderItem().getSize() == 0;

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isDirectory).and(isOnlyInTarget).and(isEmpty))
            .subscribe(pair -> createDirectory(pair.getSenderItem(), clientListener))
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
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteDirectories(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getReceiverItem().isDirectory();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isDirectory).and(isOnlyInTarget))
            .subscribe(pair -> delete(pair.getReceiverItem(), clientListener))
            ;
        // @formatter:on
    }

    /**
     * Löschen der Dateien mit relativem Pfad zum Basis-Verzeichnis.<br>
     * {@link SyncStatus#ONLY_IN_TARGET}<br>
     *
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void deleteFiles(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getReceiverItem() != null;
        Predicate<SyncPair> isFile = p -> p.getReceiverItem().isFile();
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isFile).and(isOnlyInTarget))
            .subscribe(pair -> delete(pair.getReceiverItem(), clientListener))
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
    public void generateChecksum(final EFileSystem fileSystem, final SyncItem syncItem, final LongConsumer consumerBytesRead)
    {
        if (!getOptions().isChecksum() || !syncItem.isFile())
        {
            return;
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

        Mono<String> checksum = fs.getChecksum(baseDir, syncItem.getRelativePath(), consumerBytesRead);
        syncItem.setChecksum(checksum.block());
    }

    /**
     * @see de.freese.jsync.client.Client#generateSyncItems(de.freese.jsync.filesystem.EFileSystem)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final EFileSystem fileSystem)
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

        return fs.generateSyncItems(baseDir, getOptions().isFollowSymLinks());
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
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void updateDirectories(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isDirectory = p -> p.getSenderItem().isDirectory();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isDirectory).and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
            .subscribe(pair -> update(pair.getSenderItem(), clientListener))
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
     * @param syncFlux {@link Flux}
     * @param clientListener {@link ClientListener}
     */
    protected void updateFiles(final Flux<SyncPair> syncFlux, final ClientListener clientListener)
    {
        Predicate<SyncPair> isExisting = p -> p.getSenderItem() != null;
        Predicate<SyncPair> isFile = p -> p.getSenderItem().isFile();
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncFlux
            .filter(isExisting.and(isFile).and(isOnlyInSource.or(isDifferentPermission).or(isDifferentTimestamp).or(isDifferentUser).or(isDifferentGroup)))
            .subscribe(pair -> update(pair.getSenderItem(), clientListener))
            ;
        // @formatter:on
    }
}
