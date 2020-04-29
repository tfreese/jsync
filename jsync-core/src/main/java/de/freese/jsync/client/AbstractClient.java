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
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.filesystem.target.Target;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;
import de.freese.jsync.util.JSyncUtils;

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
     * Kopieren der Dateien auf den {@link Target}<br>
     *
     * @param source {@link Source}
     * @param target {@link Target}
     * @param syncItem {@link SyncItem}
     */
    @SuppressWarnings("resource")
    protected void copyFile(final Source source, final Target target, final FileSyncItem syncItem)
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
            writableByteChannel = target.getChannel(syncItem);

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

            ByteBuffer buffer = getBuffer();

            long bytesTransferred = 0;

            // while ((readableByteChannel.read(buffer) != -1) || (fileBytesTransferred < fileSize))
            while (fileBytesTransferred < fileSize)
            {
                bytesTransferred += readableByteChannel.read(buffer);
                buffer.flip();

                getClientListener().copyFileProgress(syncItem, bytesTransferred, fileSize);

                while (buffer.hasRemaining())
                {
                    fileBytesTransferred += writableByteChannel.write(buffer);
                }

                buffer.clear();
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
            target.validateFile(syncItem);

            // Attribute aktualisieren.
            getClientListener().updateFile(getOptions(), syncItem);
            target.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }

        // Files.copy(getBase().resolve(file), ((TargetImpl) target).getBase().resolve(file), StandardCopyOption.REPLACE_EXISTING,
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
     * Kopieren der Dateien auf den {@link Target}<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_SIZE}<br>
     * {@link SyncStatus#DIFFERENT_CHECKSUM}<br>
     *
     * @param source Sender
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void copyFiles(final Source source, final Target target, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentTimestamp = p -> SyncStatus.DIFFERENT_LAST_MODIFIEDTIME.equals(p.getStatus());
        Predicate<SyncPair> isDifferentSize = p -> SyncStatus.DIFFERENT_SIZE.equals(p.getStatus());
        Predicate<SyncPair> isDifferentChecksum = p -> SyncStatus.DIFFERENT_CHECKSUM.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInSource.or(isDifferentTimestamp).or(isDifferentSize).or(isDifferentChecksum)))
                .forEach(pair -> copyFile(source,target, (FileSyncItem) pair.getSource()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Target}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     *
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void createDirectories(final Target target, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getSource() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInSource = p -> SyncStatus.ONLY_IN_SOURCE.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInSource))
                .forEach(pair -> createDirectory(target,  pair.getSource().getRelativePath()));
        //@formatter:on
    }

    /**
     * Erstellen von Verzeichnissen auf dem {@link Target}.<br>
     *
     * @param target {@link Target}
     * @param directory String
     */
    protected void createDirectory(final Target target, final String directory)
    {
        getClientListener().createDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            target.createDirectory(directory);
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
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void deleteDirectories(final Target target, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isDirectory = p -> p.getTarget() instanceof DirectorySyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isDirectory.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteDirectory(target, pair.getTarget().getRelativePath()));
        // @formatter:on
    }

    /**
     * Löscht ein Verzeichnismit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param target {@link Target}
     * @param directory String
     */
    protected void deleteDirectory(final Target target, final String directory)
    {
        getClientListener().deleteDirectory(getOptions(), directory);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            target.deleteDirectory(directory);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Löscht eine Datei mit relativem Pfad zum Basis-Verzeichnis.
     *
     * @param target {@link Target}
     * @param file String
     */
    protected void deleteFile(final Target target, final String file)
    {
        getClientListener().deleteFile(getOptions(), file);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            target.deleteFile(file);
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
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void deleteFiles(final Target target, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getTarget() instanceof FileSyncItem;
        Predicate<SyncPair> isOnlyInTarget = p -> SyncStatus.ONLY_IN_TARGET.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isOnlyInTarget))
                //.sorted(Comparator.comparing(SyncPair::getRelativePath).reversed())
                .forEach(pair -> deleteFile(target, pair.getTarget().getRelativePath()));
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
     * Vereinigt die Ergebnisse vom {@link Source} und vom {@link Target}.<br>
     * Die Einträge des Senders sind die Referenz.<br>
     * Ist ein Item im Receiver nicht enthalten, muss er kopiert werden.<br>
     * Ist ein Item nur Receiver enthalten, muss er dort gelöscht werden.<br>
     *
     * @param fileMapSource {@link Map}
     * @param fileMapTarget {@link Map}
     * @return {@link List}
     */
    protected List<SyncPair> mergeSyncItems(final Map<String, SyncItem> fileMapSource, final Map<String, SyncItem> fileMapTarget)
    {
        // @formatter:off
        List<SyncPair> fileList = fileMapSource.entrySet()
                .parallelStream()
                .map(entry -> new SyncPair(entry.getValue(), fileMapTarget.remove(entry.getKey())))
                .collect(Collectors.toList());
        // @formatter:on

        // Was jetzt noch in der Receiver-Map drin ist, muss gelöscht werden (source = null).
        fileMapTarget.forEach((key, value) -> fileList.add(new SyncPair(null, value)));

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
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Target}.<br>
     * {@link SyncStatus#ONLY_IN_SOURCE}<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_LAST_MODIFIEDTIME}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void updateDirectories(final Target target, final List<SyncPair> syncList)
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
                .forEach(pair -> updateDirectory(target, (DirectorySyncItem) pair.getSource()));
        // @formatter:on
    }

    /**
     * Aktualisieren von Verzeichniss-Attributen auf dem {@link Target}.<br>
     *
     * @param target {@link Target}
     * @param syncItem {@link DirectorySyncItem}
     */
    protected void updateDirectory(final Target target, final DirectorySyncItem syncItem)
    {
        getClientListener().updateDirectory(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            target.updateDirectory(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Target}.<br>
     *
     * @param target {@link Target}
     * @param syncItem {@link FileSyncItem}
     */
    protected void updateFile(final Target target, final FileSyncItem syncItem)
    {
        getClientListener().updateFile(getOptions(), syncItem);

        if (getOptions().isDryRun())
        {
            return;
        }

        try
        {
            target.updateFile(syncItem);
        }
        catch (Exception ex)
        {
            getClientListener().error(null, ex);
        }
    }

    /**
     * Aktualisieren von Datei-Attributen auf dem {@link Target}.<br>
     * {@link SyncStatus#DIFFERENT_PERMISSIONS}<br>
     * {@link SyncStatus#DIFFERENT_USER}<br>
     * {@link SyncStatus#DIFFERENT_GROUP}<br>
     *
     * @param target {@link Target}
     * @param syncList {@link List}
     */
    protected void updateFiles(final Target target, final List<SyncPair> syncList)
    {
        Predicate<SyncPair> isFile = p -> p.getSource() instanceof FileSyncItem;
        Predicate<SyncPair> isDifferentPermission = p -> SyncStatus.DIFFERENT_PERMISSIONS.equals(p.getStatus());
        Predicate<SyncPair> isDifferentUser = p -> SyncStatus.DIFFERENT_USER.equals(p.getStatus());
        Predicate<SyncPair> isDifferentGroup = p -> SyncStatus.DIFFERENT_GROUP.equals(p.getStatus());

        // @formatter:off
        syncList.stream()
                .filter(isFile.and(isDifferentPermission.or(isDifferentUser).or(isDifferentGroup)))
                .forEach(pair -> updateFile(target, (FileSyncItem) pair.getSource()));
        // @formatter:on
    }
}
