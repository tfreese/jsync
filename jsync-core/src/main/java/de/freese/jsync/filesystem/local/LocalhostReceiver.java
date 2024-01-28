// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.ReactiveUtils;

/**
 * {@link Receiver} für Localhost-Filesysteme.
 *
 * @author Thomas Freese
 */
public class LocalhostReceiver extends AbstractLocalFileSystem implements Receiver {
    @Override
    public void createDirectory(final String baseDir, final String relativePath) {
        final Path path = Paths.get(baseDir, relativePath);

        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks) {
        final Path path = Paths.get(baseDir, relativePath);

        try {
            JSyncUtils.delete(path, followSymLinks);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return super.generateSyncItems(baseDir, followSymLinks, PathFilterNoOp.INSTANCE);
    }

    @Override
    public void update(final String baseDir, final SyncItem syncItem) {
        final Path path = Paths.get(baseDir, syncItem.getRelativePath());

        // TimeUnit = SECONDS
        final long lastModifiedTime = syncItem.getLastModifiedTime();

        // Format "rwxr-xr-x"; optional, can be NULL on Windows.
        //        String permissions = syncItem.getPermissionsToString();

        // Optional, can be NULL on Windows.
        //        String groupName = syncItem.getGroup() == null ? null : syncItem.getGroup().getName();

        // Optional, can be NULL on Windows.
        //        String userName = syncItem.getUser() == null ? null : syncItem.getUser().getName();

        try {
            Files.setLastModifiedTime(path, FileTime.from(lastModifiedTime, TimeUnit.SECONDS));

            //            if (Options.IS_LINUX)
            //            {
            //                Set<PosixFilePermission> filePermissions = PosixFilePermissions.fromString(permissions);
            //                // FileAttribute<Set<PosixFilePermission>> fileAttributePermissions = PosixFilePermissions.asFileAttribute(filePermissions);
            //
            //                Files.setPosixFilePermissions(path, filePermissions);
            //
            //                FileSystem fileSystem = path.getFileSystem();
            //                UserPrincipalLookupService lookupService = fileSystem.getUserPrincipalLookupService();
            //
            //                PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            //                GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(groupName);
            //                fileAttributeView.setGroup(groupPrincipal);
            //
            //                UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(userName);
            //                fileAttributeView.setOwner(userPrincipal);
            //            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer consumerChecksumBytesRead) {
        final Path path = Paths.get(baseDir, syncItem.getRelativePath());

        try {
            if (Files.size(path) != syncItem.getSize()) {
                final String message = String.format("fileSize does not match with source: %s/%s", baseDir, syncItem.getRelativePath());
                throw new IllegalStateException(message);
            }

            if (withChecksum) {
                getLogger().debug("building Checksum: {}/{}", baseDir, syncItem.getRelativePath());

                final String checksum = DigestUtils.sha256DigestAsHex(path, consumerChecksumBytesRead);

                if (!checksum.equals(syncItem.getChecksum())) {
                    final String message = String.format("checksum does not match with source: %s/%s", baseDir, syncItem.getRelativePath());
                    throw new IllegalStateException(message);
                }
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux) {
        final Path path = Paths.get(baseDir, relativeFile);
        final Path parentPath = path.getParent();

        try {
            if (Files.notExists(parentPath)) {
                Files.createDirectories(parentPath);
            }

            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            final FileChannel fileChannelReceiver = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.SYNC);

            return ReactiveUtils.write(fileFlux, fileChannelReceiver).doFinally(type -> JSyncUtils.close(fileChannelReceiver));
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
