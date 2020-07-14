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
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import de.freese.jsync.Options;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.utils.DigestUtils;

/**
 * Default-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public class DefaultGenerator extends AbstractGenerator
{
    /**
     * Erzeugt eine neue Instanz von {@link DefaultGenerator}.
     */
    public DefaultGenerator()
    {
        super();
    }

    /**
     * @see de.freese.jsync.generator.Generator#createSyncItems(de.freese.jsync.Options, java.nio.file.Path,
     *      de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public NavigableMap<String, SyncItem> createSyncItems(final Options options, final Path base, final GeneratorListener listener)
    {
        if (Files.notExists(base))
        {
            return Collections.emptyNavigableMap();
        }

        FileVisitOption[] visitOption = options.isFollowSymLinks() ? FILEVISITOPTION_WITH_SYMLINKS : FILEVISITOPTION_NO_SYNLINKS;

        Set<Path> paths = getPaths(options, base, visitOption);
        listener.pathCount(base, paths.size());

        LinkOption[] linkOption = options.isFollowSymLinks() ? LINKOPTION_WITH_SYMLINKS : LINKOPTION_NO_SYMLINKS;

        NavigableMap<String, SyncItem> map = new TreeMap<>();

        // @formatter:off
        paths.stream()
                .map(path -> toItem(options, base, path, linkOption, listener))
                .forEach(syncItem -> {
                    listener.syncItem(syncItem);

                    if(options.isChecksum() && (syncItem instanceof FileSyncItem))
                    {
                        FileSyncItem fileSyncItem = (FileSyncItem)syncItem;

                        Path absolutePath = base.resolve(syncItem.getRelativePath());

                        String checksum = DigestUtils.sha256DigestAsHex(absolutePath, options.getBufferSize(), bytesRead -> listener.checksum(fileSyncItem.getSize(), bytesRead));
                        fileSyncItem.setChecksum(checksum);
                    }

                    map.put(syncItem.getRelativePath(), syncItem);
                })
        ;
        // @formatter:on

        // @formatter:off
//        NavigableMap<String, SyncItem> map = paths.stream()
//                .map(path -> toItem(options,base,path, linkOption, listener))
//                .peek(gl::processingSyncItem)
//                // .collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity()));
//                .collect(Collectors.toMap(SyncItem::getRelativePath, Function.identity(),
//                        (v1, v2) -> { throw new IllegalStateException(String.format("Duplicate key %s", v1)); },
//                        TreeMap::new)) // () -> Collections.synchronizedMap(new TreeMap<>())
//        ;
        // @formatter:on

        return map;
    }

    /**
     * @param options {@link Options}
     * @param base {@link Path}; Basis-Verzeichnis
     * @param directory {@link Path}
     * @param linkOption {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @return {@link SyncItem}
     * @throws IOException Falls was schief geht.
     */
    protected SyncItem toDirectoryItem(final Options options, final Path base, final Path directory, final LinkOption[] linkOption) throws IOException
    {
        DirectorySyncItem syncItem = new DirectorySyncItem(base.relativize(directory).toString());

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
     * @param options {@link Options}
     * @param base {@link Path}; Basis-Verzeichnis
     * @param file {@link Path}
     * @param linkOption {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @param listener {@link GeneratorListener}
     * @return {@link SyncItem}
     * @throws IOException Falls was schief geht.
     */
    protected SyncItem toFileItem(final Options options, final Path base, final Path file, final LinkOption[] linkOption, final GeneratorListener listener)
        throws IOException
    {
        FileSyncItem syncItem = new FileSyncItem(base.relativize(file).toString());

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

        return syncItem;
    }

    /**
     * Wenn die {@link Options#isFollowSymLinks} true ist, werden SymLinks verfolgt.
     *
     * @param options {@link Options}
     * @param base {@link Path}; Basis-Verzeichnis
     * @param path {@link Path}
     * @param linkOption {@link LinkOption}
     * @param listener {@link GeneratorListener}
     * @return {@link SyncItem}
     */
    protected SyncItem toItem(final Options options, final Path base, final Path path, final LinkOption[] linkOption, final GeneratorListener listener)
    {
        try
        {
            if (Files.isDirectory(path))
            {
                return toDirectoryItem(options, base, path, linkOption);
            }

            return toFileItem(options, base, path, linkOption, listener);
        }
        catch (IOException ioex)
        {
            throw new RuntimeException(ioex);
        }
    }
}
