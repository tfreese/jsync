// Created: 05.04.2018
package de.freese.jsync.filesystem.destination;

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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import de.freese.jsync.Options;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.util.DigestUtils;
import de.freese.jsync.util.JSyncUtils;

/**
 * {@link Target} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostTarget extends AbstractTarget
{
    /**
    *
    */
    private final Path base;

    /**
     * Erzeugt eine neue Instanz von {@link LocalhostTarget}.
     *
     * @param options {@link Options}
     * @param baseUri {@link URI}
     */
    public LocalhostTarget(final Options options, final URI baseUri)
    {
        super(options);

        Objects.requireNonNull(baseUri, "baseUri required");

        this.base = Paths.get(JSyncUtils.normalizedPath(baseUri));
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#connect()
     */
    @Override
    public void connect() throws Exception
    {
        // NO-OP
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#createDirectory(java.lang.String)
     */
    @Override
    public void createDirectory(final String dir) throws Exception
    {
        Path path = getBase().resolve(dir);

        getLogger().debug("create Directory: {}", path.toString());

        if (!Files.exists(path))
        {
            Files.createDirectories(path);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#createSyncItems(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public Callable<Map<String, SyncItem>> createSyncItems(final GeneratorListener listener)
    {
        getLogger().debug("create SyncItems: {}", getBase().toString());

        Generator generator = new DefaultGenerator(getOptions(), getBase());
        Callable<Map<String, SyncItem>> callable = generator.createSyncItemTasks(listener);

        return callable;
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#deleteDirectory(java.lang.String)
     */
    @Override
    public void deleteDirectory(final String dir) throws Exception
    {
        Path path = getBase().resolve(dir);

        getLogger().debug("create Directory: {}", path.toString());

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
     * @see de.freese.jsync.filesystem.destination.Target#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(final String file) throws Exception
    {
        Path path = getBase().resolve(file);

        getLogger().debug("delete File: {}", path.toString());

        Files.delete(path);
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        // NO-OP
    }

    /**
     * Liefert das Basis-Verzeichnis.
     *
     * @return base {@link Path}
     */
    protected Path getBase()
    {
        return this.base;
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final FileSyncItem syncItem) throws Exception
    {
        Path path = getBase().resolve(syncItem.getRelativePath());

        getLogger().debug("get WritableByteChannel: {}", path.toString());

        // Ist bereits erfolgt.
        // if (!Files.exists(path.getParent()))
        // {
        // Files.createDirectories(path.getParent());
        // }

        // FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        return Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Aktualisiert ein Verzeichnis oder Datei.
     *
     * @param path {@link Path}
     * @param lastModifiedTime long; TimeUnit = SECONDS
     * @param permissions String; In der Form "rwxr-xr-x"; optional, kann unter Windows null sein.
     * @param groupName String; optional, kann unter Windows null sein.
     * @param userName String; optional, kann unter Windows null sein.
     * @throws Exception Falls was schief geht.
     */
    @SuppressWarnings("resource")
    protected void update(final Path path, final long lastModifiedTime, final String permissions, final String groupName, final String userName)
        throws Exception
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

    /**
     * @see de.freese.jsync.filesystem.destination.Target#updateDirectory(de.freese.jsync.model.DirectorySyncItem)
     */
    @Override
    public void updateDirectory(final DirectorySyncItem syncItem) throws Exception
    {
        Path path = getBase().resolve(syncItem.getRelativePath());

        getLogger().debug("update Directory: {}", path.toString());

        String permissions = syncItem.getPermissionsToString();
        long lastModifiedTime = syncItem.getLastModifiedTime();
        String groupName = syncItem.getGroup() == null ? null : syncItem.getGroup().getName();
        String userName = syncItem.getUser() == null ? null : syncItem.getUser().getName();

        update(path, lastModifiedTime, permissions, groupName, userName);
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#updateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void updateFile(final FileSyncItem syncItem) throws Exception
    {
        Path path = getBase().resolve(syncItem.getRelativePath());

        getLogger().debug("update File: {}", path.toString());

        String permissions = syncItem.getPermissionsToString();
        long lastModifiedTime = syncItem.getLastModifiedTime();
        String groupName = syncItem.getGroup() == null ? null : syncItem.getGroup().getName();
        String userName = syncItem.getUser() == null ? null : syncItem.getUser().getName();

        update(path, lastModifiedTime, permissions, groupName, userName);
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#validateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void validateFile(final FileSyncItem syncItem) throws Exception
    {
        Path path = getBase().resolve(syncItem.getRelativePath());

        getLogger().debug("validate File: {}", path.toString());

        if (Files.size(path) != syncItem.getSize())
        {
            String message = String.format("fileSize does not match with source: %s", syncItem.getRelativePath());
            throw new IllegalStateException(message);
        }

        if (getOptions().isChecksum())
        {
            getLogger().debug("create Checksum: {}", path.toString());

            String checksum = DigestUtils.sha256DigestAsHex(path, getOptions().getBufferSize());

            if (!checksum.equals(syncItem.getChecksum()))
            {
                String message = String.format("checksum does not match with source: %s", syncItem.getRelativePath());
                throw new IllegalStateException(message);
            }
        }
    }
}
