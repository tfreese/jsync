// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.net.URI;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
     *
     * @param baseUri {@link URI}
     */
    public LocalhostReceiver(final URI baseUri)
    {
        super(baseUri);

        this.generator = new DefaultGenerator();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#connect()
     */
    @Override
    public void connect() throws Exception
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String)
     */
    @Override
    public void createDirectory(final String dir) throws Exception
    {
        Path path = getBasePath().resolve(dir);

        getLogger().debug("create: {}", path);

        if (!Files.exists(path))
        {
            Files.createDirectories(path);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String)
     */
    @Override
    public void deleteDirectory(final String dir) throws Exception
    {
        Path path = getBasePath().resolve(dir);

        getLogger().debug("create: {}", path);

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

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(final String file) throws Exception
    {
        Path path = getBasePath().resolve(file);

        getLogger().debug("delete: {}", path);

        Files.delete(path);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(de.freese.jsync.model.SyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final SyncItem syncItem) throws Exception
    {
        Path path = getBasePath().resolve(syncItem.getRelativePath());

        getLogger().debug("get WritableByteChannel: {}", path);

        // Ist bereits erfolgt.
        // if (!Files.exists(path.getParent()))
        // {
        // Files.createDirectories(path.getParent());
        // }

        // FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        return Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String relativePath, final LongConsumer consumerBytesRead) throws Exception
    {
        String checksum = this.generator.generateChecksum(getBasePath().toString(), relativePath, consumerBytesRead);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getSyncItems(boolean)
     */
    @Override
    public List<SyncItem> getSyncItems(final boolean followSymLinks)
    {
        List<SyncItem> items = this.generator.generateItems(getBasePath().toString(), followSymLinks);

        return items;
    }

    /**
     * Aktualisiert ein Verzeichnis oder Datei.
     *
     * @param path {@link Path}
     * @param syncItem {@link SyncItem}
     * @throws Exception Falls was schief geht.
     */
    @SuppressWarnings("resource")
    protected void update(final Path path, final SyncItem syncItem) throws Exception
    {
        // In der Form "rwxr-xr-x"; optional, kann unter Windows null sein.
        String permissions = syncItem.getPermissionsToString();

        // TimeUnit = SECONDS
        long lastModifiedTime = syncItem.getLastModifiedTime();

        // Optional, kann unter Windows null sein.
        String groupName = syncItem.getGroup() == null ? null : syncItem.getGroup().getName();

        // Optional, kann unter Windows null sein.
        String userName = syncItem.getUser() == null ? null : syncItem.getUser().getName();

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

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final SyncItem syncItem) throws Exception
    {
        Path path = getBasePath().resolve(syncItem.getRelativePath());

        getLogger().debug("update: {}", path);

        update(path, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final SyncItem syncItem) throws Exception
    {
        Path path = getBasePath().resolve(syncItem.getRelativePath());

        getLogger().debug("update: {}", path);

        update(path, syncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final SyncItem syncItem, final boolean withChecksum) throws Exception
    {
        Path path = getBasePath().resolve(syncItem.getRelativePath());

        getLogger().debug("validate: {}", path);

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
}
