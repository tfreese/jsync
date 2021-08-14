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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import de.freese.jsync.Options;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;
import reactor.core.publisher.Flux;

/**
 * Default-Implementierung des {@link Generator}.
 *
 * @author Thomas Freese
 */
public class DefaultGenerator extends AbstractGenerator
{
    /**
     * @see de.freese.jsync.generator.Generator#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        Path path = Paths.get(baseDir, relativeFile);

        return DigestUtils.sha256DigestAsHex(path, consumerChecksumBytesRead);
    }

    /**
     * @param path {@link Path}
     * @param relativePath String
     * @param linkOptions {@link LinkOption}[]
     *
     * @return {@link SyncItem}
     */
    public SyncItem generateItem(final Path path, final String relativePath, final LinkOption[] linkOptions)
    {
        if (Files.isDirectory(path))
        {
            return toDirectoryItem(path, relativePath, linkOptions);
        }

        return toFileItem(path, relativePath, linkOptions);
    }

    /**
     * @see de.freese.jsync.generator.Generator#generateItems(java.lang.String, boolean)
     */
    @Override
    public Flux<SyncItem> generateItems(final String baseDir, final boolean followSymLinks)
    {
        Path base = Paths.get(baseDir);

        if (Files.notExists(base))
        {
            Flux.empty();
        }

        FileVisitOption[] visitOptions = JSyncUtils.getFileVisitOptions(followSymLinks);
        LinkOption[] linkOptions = JSyncUtils.getLinkOptions(followSymLinks);

        // @formatter:off
        return getPathsAsFlux(base, visitOptions)
            .map(path -> {
                String relativePath = base.relativize(path).toString();

                SyncItem syncItem = generateItem(path, relativePath, linkOptions);

                return syncItem;
            })
            ;
        // @formatter:on
    }

    /**
     * @param directory {@link Path}
     * @param relativeDir String
     * @param linkOptions {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     *
     * @return {@link SyncItem}
     */
    protected SyncItem toDirectoryItem(final Path directory, final String relativeDir, final LinkOption[] linkOptions)
    {
        SyncItem syncItem = new DefaultSyncItem(relativeDir);

        try
        {
            // syncItem.setSize(Files.walk(directory, 1).count());
            Predicate<Path> self = p -> p.getFileName().toString().startsWith(".");

            try (Stream<Path> childs = Files.list(directory).filter(self.negate()))
            {
                syncItem.setSize(childs.count());
            }

            if (Options.IS_WINDOWS)
            {
                long lastModifiedTime = Files.getLastModifiedTime(directory, linkOptions).to(TimeUnit.SECONDS);

                syncItem.setLastModifiedTime(lastModifiedTime);
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

                syncItem.setLastModifiedTime(lastModifiedTime);
                syncItem.setPermissions(filePermissions);
                syncItem.setUser(new User(userName, uid));
                syncItem.setGroup(new Group(groupName, gid));

                // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
                // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        return syncItem;
    }

    /**
     * @param file {@link Path}
     * @param relativeFile String
     * @param linkOptions {@link LinkOption}; wenn {@value LinkOption#NOFOLLOW_LINKS} null dann Follow
     *
     * @return {@link SyncItem}
     */
    protected SyncItem toFileItem(final Path file, final String relativeFile, final LinkOption[] linkOptions)
    {
        SyncItem syncItem = new DefaultSyncItem(relativeFile);
        syncItem.setFile(true);

        try
        {
            if (Options.IS_WINDOWS)
            {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);

                long lastModifiedTime = basicFileAttributes.lastModifiedTime().to(TimeUnit.SECONDS);
                long size = basicFileAttributes.size();

                syncItem.setLastModifiedTime(lastModifiedTime);
                syncItem.setSize(size);
            }
            else if (Options.IS_LINUX)
            {
                // PosixFileAttributes basicFileAttributes = Files.readAttributes(file, PosixFileAttributes.class, linkOption);

                // posix:*, basic:*, unix:*
                Map<String, Object> attributes = Files.readAttributes(file, "unix:lastModifiedTime,size,permissions,owner,group,uid,gid", linkOptions);

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
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        return syncItem;
    }
}
