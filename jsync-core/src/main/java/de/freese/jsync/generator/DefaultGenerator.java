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
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

import reactor.core.publisher.Flux;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class DefaultGenerator extends AbstractGenerator {
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        final Path path = Paths.get(baseDir, relativeFile);

        return DigestUtils.sha256DigestAsHex(path, consumerChecksumBytesRead);
    }

    @Override
    public Flux<SyncItem> generateItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        final Path base = Paths.get(baseDir);

        if (Files.notExists(base)) {
            return Flux.empty();
        }

        final FileVisitOption[] visitOptions = JSyncUtils.getFileVisitOptions(followSymLinks);
        final LinkOption[] linkOptions = JSyncUtils.getLinkOptions(followSymLinks);

        return getPathsAsFlux(base, visitOptions, pathFilter)
                .mapNotNull(path -> {
                    if (Files.isDirectory(path)) {
                        return toDirectoryItem(path, base.relativize(path).toString(), linkOptions);
                    }

                    return toFileItem(path, base.relativize(path).toString(), linkOptions);
                })
                ;
    }

    /**
     * @param linkOptions {@link LinkOption}; if LinkOption#NOFOLLOW_LINKS null than Follow
     */
    protected SyncItem toDirectoryItem(final Path directory, final String relativeDir, final LinkOption[] linkOptions) {
        if (relativeDir.isEmpty()) {
            // relativeDir = directory
            return null;
        }

        final SyncItem syncItem = new DefaultSyncItem(relativeDir);

        try {
            try (Stream<Path> children = Files.list(directory)) {

                final long count = children
                        .filter(child -> !child.equals(directory)) // We do not want the Base-Directory.
                        .count();

                syncItem.setSize(count);
            }

            final long lastModifiedTime = Files.getLastModifiedTime(directory, linkOptions).to(TimeUnit.SECONDS);
            syncItem.setLastModifiedTime(lastModifiedTime);

            // if (Options.IS_WINDOWS) {
            //     final long lastModifiedTime = Files.getLastModifiedTime(directory, linkOptions).to(TimeUnit.SECONDS);
            //
            //     syncItem.setLastModifiedTime(lastModifiedTime);
            // }
            // else if (Options.IS_LINUX) {
            //     // unix:mode
            //     final Map<String, Object> attributes = Files.readAttributes(directory, "unix:lastModifiedTime,permissions,owner,group,uid,gid", linkOptions);
            //
            //     final long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);
            //
            //     final Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");
            //
            //     final String userName = ((UserPrincipal) attributes.get("owner")).getName();
            //     final String groupName = ((GroupPrincipal) attributes.get("group")).getName();
            //     final int uid = (int) attributes.get("uid");
            //     final int gid = (int) attributes.get("gid");
            //
            //     syncItem.setLastModifiedTime(lastModifiedTime);
            //     syncItem.setPermissions(filePermissions);
            //     syncItem.setUser(new User(userName, uid));
            //     syncItem.setGroup(new Group(groupName, gid));
            //
            //     // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
            //     // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
            // }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return syncItem;
    }

    /**
     * @param linkOptions {@link LinkOption}; if LinkOption#NOFOLLOW_LINKS null than Follow
     */
    protected SyncItem toFileItem(final Path file, final String relativeFile, final LinkOption[] linkOptions) {
        final SyncItem syncItem = new DefaultSyncItem(relativeFile);
        syncItem.setFile(true);

        try {
            final BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);
            syncItem.setLastModifiedTime(basicFileAttributes.lastModifiedTime().to(TimeUnit.SECONDS));
            syncItem.setSize(basicFileAttributes.size());

            // if (Options.IS_WINDOWS) {
            //     final BasicFileAttributes basicFileAttributes = Files.readAttributes(file, BasicFileAttributes.class, linkOptions);
            //
            //     final long lastModifiedTime = basicFileAttributes.lastModifiedTime().to(TimeUnit.SECONDS);
            //     final long size = basicFileAttributes.size();
            //
            //     syncItem.setLastModifiedTime(lastModifiedTime);
            //     syncItem.setSize(size);
            // }
            // else if (Options.IS_LINUX) {
            //     // PosixFileAttributes basicFileAttributes = Files.readAttributes(file, PosixFileAttributes.class, linkOption);
            //
            //     // posix:*, basic:*, unix:*
            //     final Map<String, Object> attributes = Files.readAttributes(file, "unix:lastModifiedTime,size,permissions,owner,group,uid,gid", linkOptions);
            //
            //     final long lastModifiedTime = ((FileTime) attributes.get("lastModifiedTime")).to(TimeUnit.SECONDS);
            //     final long size = (long) attributes.get("size");
            //
            //     @SuppressWarnings("unchecked") final Set<PosixFilePermission> filePermissions = (Set<PosixFilePermission>) attributes.get("permissions");
            //
            //     final String userName = ((UserPrincipal) attributes.get("owner")).getName();
            //     final String groupName = ((GroupPrincipal) attributes.get("group")).getName();
            //     final int uid = (int) attributes.get("uid");
            //     final int gid = (int) attributes.get("gid");
            //
            //     syncItem.setLastModifiedTime(lastModifiedTime);
            //     syncItem.setSize(size);
            //     syncItem.setPermissions(filePermissions);
            //     syncItem.setUser(new User(userName, uid));
            //     syncItem.setGroup(new Group(groupName, gid));
            //
            //     // UserPrincipalLookupService lookupService = provider(path).getUserPrincipalLookupService();
            //     // UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
            // }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return syncItem;
    }
}
