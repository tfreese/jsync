// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.util.DigestUtils;

/**
 * Default-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public class DefaultGenerator extends AbstractGenerator
{
    /**
     * Erzeugt eine neue Instanz von {@link DefaultGenerator}.
     *
     * @param options {@link Options}
     * @param base {@link Path}
     */
    public DefaultGenerator(final Options options, final Path base)
    {
        super(options, base);
    }

    /**
     * @see de.freese.jsync.generator.Generator#createSyncItemTasks(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public Callable<Map<String, SyncItem>> createSyncItemTasks(final GeneratorListener listener)
    {
        if (Files.notExists(getBase()))
        {
            // return CompletableFuture.completedFuture(Collections.emptyMap());
            return Collections::emptyMap;
        }

        // @formatter:off
        return () -> {

            FileVisitOption[] visitOption = getOptions().isFollowSymLinks() ? FILEVISITOPTION_WITH_SYMLINKS : FILEVISITOPTION_NO_SYNLINKS;

            final Set<Path> paths = getPaths(getOptions(), getBase(), visitOption);

            if(listener != null)
            {
                listener.pathCount(getBase(), paths.size());
            }

            LinkOption[] linkOption = getOptions().isFollowSymLinks() ? LINKOPTION_WITH_SYMLINKS : LINKOPTION_NO_SYMLINKS;

            return paths.stream()
                    .map(path -> toItem(path, linkOption))
                    .peek(syncItem -> {
                        if(listener != null)
                        {
                            listener.processingSyncItem(syncItem);
                        }
                    })
                    // .collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity()));
                    .collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity(),
                            (v1, v2) -> { throw new IllegalStateException(String.format("Duplicate key %s", v1)); },
                            TreeMap::new)); // () -> Collections.synchronizedMap(new TreeMap<>())
        };
        // @formatter:on
    }

    /**
     * @param directory {@link Path}
     * @param linkOption {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @return {@link SyncItem}
     * @throws IOException Falls was schief geht.
     */
    protected SyncItem toDirectoryItem(final Path directory, final LinkOption[] linkOption) throws IOException
    {
        DirectorySyncItem syncItem = new DirectorySyncItem(getBase().relativize(directory).toString());

        if (Options.IS_WINDOWS)
        {
            long lastModifiedTime = Files.getLastModifiedTime(directory, linkOption).to(TimeUnit.SECONDS);

            syncItem.setLastModifiedTime(lastModifiedTime);
        }
        else if (Options.IS_LINUX)
        {
            // unix:mode
            Map<String, Object> attributes = Files.readAttributes(directory, "unix:lastModifiedTime,permissions,owner,group,uid,gid", linkOption);

            long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);

            @SuppressWarnings("unchecked")
            Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");

            String userName = ((UserPrincipal) attributes.get("owner")).getName();
            String groupName = ((GroupPrincipal) attributes.get("group")).getName();
            int uid = (int) attributes.get("uid");
            int gid = (int) attributes.get("gid");

            syncItem.setLastModifiedTime(lastModifiedTime);
            syncItem.setPermissions(filePermissions);
            syncItem.setUser(new User(userName, uid));
            syncItem.setGroup(new Group(groupName, gid));

            // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
            // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
        }

        return syncItem;
    }

    /**
     * @param file {@link Path}
     * @param linkOption {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @return {@link SyncItem}
     * @throws IOException Falls was schief geht.
     */
    protected SyncItem toFileItem(final Path file, final LinkOption[] linkOption) throws IOException
    {
        FileSyncItem syncItem = new FileSyncItem(getBase().relativize(file).toString());

        if (Options.IS_WINDOWS)
        {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, linkOption);

            long lastModifiedTime = basicFileAttributes.lastModifiedTime().to(TimeUnit.SECONDS);
            long size = basicFileAttributes.size();

            syncItem.setLastModifiedTime(lastModifiedTime);
            syncItem.setSize(size);
        }
        else if (Options.IS_LINUX)
        {
            // PosixFileAttributes basicFileAttributes = Files.readAttributes(file, PosixFileAttributes.class, linkOption);

            // posix:*, basic:*, unix:*
            Map<String, Object> attributes = Files.readAttributes(file, "unix:lastModifiedTime,size,permissions,owner,group,uid,gid", linkOption);

            long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);
            long size = (long) attributes.get("size");

            @SuppressWarnings("unchecked")
            Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");

            String userName = ((UserPrincipal) attributes.get("owner")).getName();
            String groupName = ((GroupPrincipal) attributes.get("group")).getName();
            int uid = (int) attributes.get("uid");
            int gid = (int) attributes.get("gid");

            syncItem.setLastModifiedTime(lastModifiedTime);
            syncItem.setSize(size);
            syncItem.setPermissions(filePermissions);
            syncItem.setUser(new User(userName, uid));
            syncItem.setGroup(new Group(groupName, gid));

            // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
            // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
        }

        if (getOptions().isChecksum())
        {
            String checksum = DigestUtils.sha256DigestAsHex(file, getOptions().getBufferSize());
            syncItem.setChecksum(checksum);
        }

        return syncItem;
    }

    /**
     * Wenn die {@link Options#isFollowSymLinks} true ist, werden SymLinks verfolgt.
     *
     * @param path {@link Path}
     * @param linkOption {@link LinkOption}
     * @return {@link SyncItem}
     */
    protected SyncItem toItem(final Path path, final LinkOption[] linkOption)
    {
        try
        {
            if (Files.isDirectory(path))
            {
                return toDirectoryItem(path, linkOption);
            }

            return toFileItem(path, linkOption);
        }
        catch (IOException ioex)
        {
            throw new RuntimeException(ioex);
        }
    }
}
