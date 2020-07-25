// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;

/**
 * {@link Receiver} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostReceiver extends AbstractReceiver
{
    /**
    *
    */
    private final Generator generator;

    /**
     * Erzeugt eine neue Instanz von {@link LocalhostReceiver}.
     */
    public LocalhostReceiver()
    {
        super();

        this.generator = new DefaultGenerator();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteDirectory(final String baseDir, final String relativeDir)
    {
        Path path = Paths.get(baseDir, relativeDir);

        getLogger().debug("delete: {}", path);

        try
        {
            JSyncUtils.deleteDirectoryRecursive(path);

            if (Files.exists(path))
            {
                Files.delete(path);
            }

            // Da die Verzeichnisse immer am Ende nach den Dateien gelöscht werden, dürfte dies hier nicht notwendig sein.
            //
            // Path parent = path.getParent();
            //
            // try (Stream<Path> stream = Files.list(parent))
            // {
            // long fileCount = stream.count();
            //
            // if (fileCount == 0)
            // {
            // Files.delete(parent);
            // }
            // }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteFile(final String baseDir, final String relativeFile)
    {
        Path path = Paths.get(baseDir, relativeFile);

        getLogger().debug("delete: {}", path);

        try
        {
            Files.delete(path);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
     */
    @Override
    public void disconnect()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        this.generator.generateItems(baseDir, followSymLinks, consumerSyncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        Path path = Paths.get(baseDir, relativeFile);

        try
        {
            if (Files.notExists(path))
            {
                Files.createDirectories(path);
            }

            getLogger().debug("get WritableByteChannel: {}", path);

            // FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            return Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        String checksum = this.generator.generateChecksum(baseDir, relativeFile, consumerBytesRead);

        return checksum;
    }

    /**
     * Aktualisiert ein Verzeichnis oder Datei.
     *
     * @param path {@link Path}
     * @param syncItem {@link SyncItem}
     */
    @SuppressWarnings("resource")
    protected void update(final Path path, final SyncItem syncItem)
    {
        // In der Form "rwxr-xr-x"; optional, kann unter Windows null sein.
        String permissions = syncItem.getPermissionsToString();

        // TimeUnit = SECONDS
        long lastModifiedTime = syncItem.getLastModifiedTime();

        // Optional, kann unter Windows null sein.
        String groupName = syncItem.getGroup() == null ? null : syncItem.getGroup().getName();

        // Optional, kann unter Windows null sein.
        String userName = syncItem.getUser() == null ? null : syncItem.getUser().getName();

        try
        {
            Files.setLastModifiedTime(path, FileTime.from(lastModifiedTime, TimeUnit.SECONDS));

            if (Options.IS_LINUX)
            {
                Set<PosixFilePermission> filePermissions = PosixFilePermissions.fromString(permissions);
                // FileAttribute<Set<PosixFilePermission>> fileAttributePermissions = PosixFilePermissions.asFileAttribute(filePermissions);

                Files.setPosixFilePermissions(path, filePermissions);

                UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();

                PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
                GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(groupName);
                fileAttributeView.setGroup(groupPrincipal);

                UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(userName);
                fileAttributeView.setOwner(userPrincipal);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final String baseDir, final SyncItem syncItem)
    {
        Path path = Paths.get(baseDir, syncItem.getRelativePath());

        getLogger().debug("update: {}", path);

        update(path, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final String baseDir, final SyncItem syncItem)
    {
        Path path = Paths.get(baseDir, syncItem.getRelativePath());

        getLogger().debug("update: {}", path);

        update(path, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        Path path = Paths.get(baseDir, syncItem.getRelativePath());

        getLogger().debug("validate: {}", path);

        try
        {
            if (Files.size(path) != syncItem.getSize())
            {
                String message = String.format("fileSize does not match with source: %s", syncItem.getRelativePath());
                throw new IllegalStateException(message);
            }

            if (withChecksum)
            {
                getLogger().debug("building Checksum: {}", path);

                String checksum = DigestUtils.sha256DigestAsHex(path, Options.BUFFER_SIZE, null);

                if (!checksum.equals(syncItem.getChecksum()))
                {
                    String message = String.format("checksum does not match with source: %s", syncItem.getRelativePath());
                    throw new IllegalStateException(message);
                }
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
