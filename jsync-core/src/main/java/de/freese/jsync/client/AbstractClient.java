// Created: 05.04.2018
package de.freese.jsync.client;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.sink.Sink;
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.utils.JSyncUtils;

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
    private final ByteBuffer buffer;

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
        this.buffer = ByteBuffer.allocateDirect(this.options.getBufferSize());
    }

    /**
     * Kopieren der Dateien von der Quelle in die Senke<br>
     *
     * @param source {@link Source}
     * @param sink {@link Sink}
     * @param syncItem {@link SyncItem}
     */
    @SuppressWarnings("resource")
    protected void copyFile(final Source source, final Sink sink, final FileSyncItem syncItem)
    {
        getClientListener().copyFile(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        ReadableByteChannel readableByteChannel = null;
        WritableByteChannel writableByteChannel = null;

        try
        {
            long fileSize = syncItem.getSize();
            long fileBytesTransferred = 0;

            readableByteChannel = source.getChannel(syncItem);
            writableByteChannel = sink.getChannel(syncItem);

            if ((readableByteChannel == null) || (writableByteChannel == null))
            {
                return;
            }

            // if (getLogger().isInfoEnabled())
            // {
            // BiConsumer<Long, Long> monitorRead = (read, gesamt) -> {
            // String message = String.format("Read data for %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(read),
            // JSyncUtils.getPercent(read, gesamt));
            //
            // message = "\r\t" + message;
            //
            // // getLogger().debug(message);
            // };
            //
            // BiConsumer<Long, Long> monitorWrite = (written, gesamt) -> {
            // String message = String.format("Written data for %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(written),
            // JSyncUtils.getPercent(written, gesamt));
            //
            // message = "\r\t" + message;
            //
            // // getLogger().debug(message);
            // };
            //
            // // readableByteChannel = new MonitoringReadableByteChannel(readableByteChannel, monitorRead, fileSize);
            // // writableByteChannel = new MonitoringWritableByteChannel(writableByteChannel, monitorWrite, fileSize);
            // }

            long bytesTransferred = 0;

            // while ((readableByteChannel.read(buffer) != -1) || (fileBytesTransferred < fileSize))
            while (fileBytesTransferred < fileSize)
            {
                bytesTransferred += readableByteChannel.read(getBuffer());
                getBuffer().flip();

                getClientListener().copyFileProgress(syncItem, bytesTransferred, fileSize);

                while (getBuffer().hasRemaining())
                {
                    fileBytesTransferred += writableByteChannel.write(getBuffer());
                }

                getBuffer().clear();
            }
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
        finally
        {
            JSyncUtils.closeSilently(readableByteChannel);
            JSyncUtils.closeSilently(writableByteChannel);
        }

        getClientListener().copyFileFinished(syncItem);

        try
        {
            // Datei überprüfen.
            getClientListener().validateFile(getOptions(), syncItem);
            sink.validateFile(syncItem);

            // Attribute aktualisieren.
            getClientListener().updateFile(getOptions(), syncItem);
            sink.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }

        // Files.copy(getBase().resolve(file), ((SinkImpl) sink).getBase().resolve(file), StandardCopyOption.REPLACE_EXISTING,
        // StandardCopyOption.COPY_ATTRIBUTES);
        //
        // try (FileInputStream inStream = new FileInputStream(aSourceFile);
        // FileChannel inChannel inChannel = inStream.getChannel();
        // FileOutputStream outStream = new FileOutputStream(aTargetFile);
        // FileChannel outChannel = outStream.getChannel())
        // {
        // long bytesTransferred = 0;
        //
        // while (bytesTransferred < inChannel.size())
        // {
        // bytesTransferred += inChannel.transferTo(bytesTransferred, inChannel.size(), outChannel);
        // }
        // }
    }

    /**
     * Kopieren der Dateien auf den {@link Sink}<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_SIZE}<br>
     * {@link SyncStatus#DIFFERENT_CHECKSUM}<br>
     *
     * @param source Sender
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void copyFiles(final Source source, final Sink sink, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
                .forEach(pair -> copyFile(source,sink, (FileSyncItem) pair.getSource()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Sink}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void createDirectories(final Sink sink, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getSource() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInSource))
                .forEach(pair -> createDirectory(sink,  pair.getSource().getRelativePath()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Sink}.<br>
     *
     * @param sink {@link Sink}
     * @param directory String
     */
    protected void createDirectory(final Sink sink, final String directory)
    {
        getClientListener().createDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            sink.createDirectory(directory);
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
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void deleteDirectories(final Sink sink, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getTarget() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteDirectory(sink, pair.getTarget().getRelativePath()));
        // @formatter:on
    }

    /**
     * Löscht ein Verzeichnismit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param sink {@link Sink}
     * @param directory String
     */
    protected void deleteDirectory(final Sink sink, final String directory)
    {
        getClientListener().deleteDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            sink.deleteDirectory(directory);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Löscht eine Datei mit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param sink {@link Sink}
     * @param file String
     */
    protected void deleteFile(final Sink sink, final String file)
    {
        getClientListener().deleteFile(getOptions(), file);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            sink.deleteFile(file);
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
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void deleteFiles(final Sink sink, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getTarget() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteFile(sink, pair.getTarget().getRelativePath()));
        // @formatter:on
    }

    /**
     * @return {@link ByteBuffer}
     */
    protected ByteBuffer getBuffer()
    {
        this.buffer.clear();

        return this.buffer;
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
     * Vereinigt die Ergebnisse vom {@link Source} und vom {@link Sink}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param fileMapSource {@link Map}
     * @param fileMapSink {@link Map}
     * @return {@link List}
     */
    protected List<SyncPair> mergeSyncItems(final Map<String, SyncItem> fileMapSource, final Map<String, SyncItem> fileMapSink)
    {
        // @formatter:off
        List<SyncPair> fileList = fileMapSource.entrySet()
                .parallelStream()
                .map(entry -> new SyncPair(entry.getValue(), fileMapSink.remove(entry.getKey())))
                .collect(Collectors.toList());
        // @formatter:on

        // Was jetzt noch in der Receiver-Map drin ist, muss gelöscht werden (source = null).
        fileMapSink.forEach((key, value) -> fileList.add(new SyncPair(null, value)));

        // SyncStatus ermitteln.
        // @formatter:off
        fileList.stream()
                .parallel()
                .peek(SyncPair::validateStatus)
                .sequential()
                .forEach(getClientListener()::debugSyncPair);
        // @formatter:on

        return fileList;
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Sink}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void updateDirectories(final Sink sink, final List<SyncPair> syncList)
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
                .forEach(pair -> updateDirectory(sink, (DirectorySyncItem) pair.getSource()));
        // @formatter:on
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Sink}.<br>
     *
     * @param sink {@link Sink}
     * @param syncItem {@link DirectorySyncItem}
     */
    protected void updateDirectory(final Sink sink, final DirectorySyncItem syncItem)
    {
        getClientListener().updateDirectory(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            sink.updateDirectory(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Sink}.<br>
     *
     * @param sink {@link Sink}
     * @param syncItem {@link FileSyncItem}
     */
    protected void updateFile(final Sink sink, final FileSyncItem syncItem)
    {
        getClientListener().updateFile(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            sink.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Sink}.<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param sink {@link Sink}
     * @param syncList {@link List}
     */
    protected void updateFiles(final Sink sink, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isDifferentPermission.or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateFile(sink, (FileSyncItem) pair.getSource()));
        // @formatter:on
    }
}
