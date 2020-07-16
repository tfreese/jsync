// Created: 05.04.2018
package de.freese.jsync.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import de.freese.jsync.Options;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncItemMeta;
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
     * @see de.freese.jsync.generator.Generator#generateItems(java.lang.String, boolean, de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public List<SyncItem> generateItems(final String basePath, final boolean followSymLinks, final GeneratorListener listener)
    {
        Path base = Paths.get(basePath);

        if (Files.notExists(base))
        {
            return Collections.emptyList();
        }

        FileVisitOption[] visitOptions = followSymLinks ? FILEVISITOPTION_WITH_SYMLINKS : FILEVISITOPTION_NO_SYNLINKS;

        Set<Path> paths = getPaths(base, visitOptions);

        listener.itemCount(base, paths.size());

        List<SyncItem> syncItems = paths.stream().map(p -> new DefaultSyncItem(base.relativize(p).toString())).collect(Collectors.toList());

        return syncItems;
    }

    /**
     * @see de.freese.jsync.generator.Generator#generateMeta(java.lang.String, java.lang.String, boolean, boolean,
     *      de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public SyncItemMeta generateMeta(final String basePath, final String relativePath, final boolean followSymLinks, final boolean withChecksum,
                                     final GeneratorListener listener)
    {
        Path path = Paths.get(basePath).resolve(relativePath);

        if (Files.notExists(path))
        {
            getLogger().warn("Path not exist: {}", path);

            return null;
        }

        listener.currentMeta(relativePath);

        LinkOption[] linkOptions = followSymLinks ? LINKOPTION_WITH_SYMLINKS : LINKOPTION_NO_SYMLINKS;
        SyncItemMeta meta = null;

        if (Files.isDirectory(path))
        {
            meta = toDirectoryItem(path, linkOptions);
        }
        else
        {
            meta = toFileItem(path, linkOptions, withChecksum, listener);
        }

        return meta;
    }

    /**
     * @param directory {@link Path}
     * @param linkOptions {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @return {@link SyncItemMeta}
     */
    protected SyncItemMeta toDirectoryItem(final Path directory, final LinkOption[] linkOptions)
    {
        SyncItemMeta meta = new SyncItemMeta();

        try
        {
            if (Options.IS_WINDOWS)
            {
                long lastModifiedTime = Files.getLastModifiedTime(directory, linkOptions).to(TimeUnit.SECONDS);

                meta.setLastModifiedTime(lastModifiedTime);
            }
            else if (Options.IS_LINUX)
            {
                // unix:mode
                Map<String, Object> attributes = Files.readAttributes(directory, "unix:lastModifiedTime,permissions,owner,group,uid,gid", linkOptions);

                long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);

                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");

                String userName = ((UserPrincipal) attributes.get("owner")).getName();
                String groupName = ((GroupPrincipal) attributes.get("group")).getName();
                int uid = (int) attributes.get("uid");
                int gid = (int) attributes.get("gid");

                meta.setLastModifiedTime(lastModifiedTime);
                meta.setPermissions(filePermissions);
                meta.setUser(new User(userName, uid));
                meta.setGroup(new Group(groupName, gid));

                // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
                // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
            }
        }
        catch (IOException ioex)
        {
            throw new UncheckedIOException(ioex);
        }

        return meta;
    }

    /**
     * @param file {@link Path}
     * @param linkOptions {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     * @param withChecksum boolean checksum,
     * @param listener {@link GeneratorListener}
     * @return {@link SyncItemMeta}
     */
    protected SyncItemMeta toFileItem(final Path file, final LinkOption[] linkOptions, final boolean withChecksum, final GeneratorListener listener)
    {
        SyncItemMeta meta = new SyncItemMeta();
        meta.setFile(true);

        try
        {
            long size = 0L;

            if (Options.IS_WINDOWS)
            {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);

                long lastModifiedTime = basicFileAttributes.lastModifiedTime().to(TimeUnit.SECONDS);
                size = basicFileAttributes.size();

                meta.setLastModifiedTime(lastModifiedTime);
                meta.setSize(size);
            }
            else if (Options.IS_LINUX)
            {
                // PosixFileAttributes basicFileAttributes = Files.readAttributes(file, PosixFileAttributes.class, linkOption);

                // posix:*, basic:*, unix:*
                Map<String, Object> attributes = Files.readAttributes(file, "unix:lastModifiedTime,size,permissions,owner,group,uid,gid", linkOptions);

                long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);
                size = (long) attributes.get("size");

                @SuppressWarnings("unchecked")
                Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");

                String userName = ((UserPrincipal) attributes.get("owner")).getName();
                String groupName = ((GroupPrincipal) attributes.get("group")).getName();
                int uid = (int) attributes.get("uid");
                int gid = (int) attributes.get("gid");

                meta.setLastModifiedTime(lastModifiedTime);
                meta.setSize(size);
                meta.setPermissions(filePermissions);
                meta.setUser(new User(userName, uid));
                meta.setGroup(new Group(groupName, gid));

                // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
                // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
            }

            if (withChecksum)
            {
                String checksum = DigestUtils.sha256DigestAsHex(file, Options.BUFFER_SIZE, bytesRead -> listener.checksum(meta.getSize(), bytesRead));
                meta.setChecksum(checksum);
            }
        }
        catch (IOException ioex)
        {
            throw new UncheckedIOException(ioex);
        }

        return meta;
    }
}
