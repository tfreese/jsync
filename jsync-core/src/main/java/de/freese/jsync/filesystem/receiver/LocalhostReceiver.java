// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.FileHandle;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.io.MonitoringWritableByteChannel;

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
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        getLogger().info("create: {}/{}", baseDir, relativePath);

        Path path = Paths.get(baseDir, relativePath);

        try
        {
            if (Files.notExists(path))
            {
                Files.createDirectories(path);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        getLogger().info("delete: {}/{}", baseDir, relativePath);

        Path path = Paths.get(baseDir, relativePath);

        try
        {
            JSyncUtils.delete(path, followSymLinks);
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
        getLogger().debug("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        this.generator.generateItems(baseDir, followSymLinks, consumerSyncItem);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        getLogger().debug("create checksum: {}/{}", baseDir, relativeFile);

        String checksum = this.generator.generateChecksum(baseDir, relativeFile, consumerBytesRead);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getResource(java.lang.String, java.lang.String, long)
     */
    @Override
    public WritableResource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        getLogger().info("get resource: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        Path path = Paths.get(baseDir, relativeFile);
        Path parentPath = path.getParent();

        try
        {
            if (Files.notExists(parentPath))
            {
                Files.createDirectories(parentPath);
            }

            if (Files.notExists(path))
            {
                Files.createFile(path);
            }

            // FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            // Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // return new PathResource(path);
            return new FileSystemResource(path); // Liefert FileChannel
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @SuppressWarnings("resource")
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        getLogger().info("update: {}/{}", baseDir, syncItem.getRelativePath());

        Path path = Paths.get(baseDir, syncItem.getRelativePath());

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

                FileSystem fileSystem = path.getFileSystem();
                UserPrincipalLookupService lookupService = fileSystem.getUserPrincipalLookupService();

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
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        getLogger().debug("validate file: {}/{}, withChecksum={}", baseDir, syncItem.getRelativePath(), withChecksum);

        Path path = Paths.get(baseDir, syncItem.getRelativePath());

        try
        {
            if (Files.size(path) != syncItem.getSize())
            {
                String message = String.format("fileSize does not match with source: %s/%s", baseDir, syncItem.getRelativePath());
                throw new IllegalStateException(message);
            }

            if (withChecksum)
            {
                getLogger().debug("building Checksum: {}/{}", baseDir, syncItem.getRelativePath());

                String checksum = DigestUtils.sha256DigestAsHex(path, Options.DATABUFFER_SIZE);

                if (!checksum.equals(syncItem.getChecksum()))
                {
                    String message = String.format("checksum does not match with source: %s/%s", baseDir, syncItem.getRelativePath());
                    throw new IllegalStateException(message);
                }
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void writeChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer buffer)
    {
        getLogger().debug("write chunk: {}/{}, position={}, sizeOfChunk={}", baseDir, relativeFile, position, sizeOfChunk);

        if (sizeOfChunk > buffer.capacity())
        {
            throw new IllegalArgumentException("size > buffer.capacity()");
        }

        Path path = Paths.get(baseDir, relativeFile);

        Path parentPath = path.getParent();

        try
        {
            if (Files.notExists(parentPath))
            {
                Files.createDirectories(parentPath);
            }

            if (buffer.position() != 0)
            {
                buffer.flip();
            }

            try (FileChannel fileChannel =
                    (FileChannel) Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
            {
                fileChannel.write(buffer, position);

                fileChannel.force(false);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeFileHandle(java.lang.String, java.lang.String, long, de.freese.jsync.filesystem.FileHandle,
     *      java.util.function.LongConsumer)
     */
    @Override
    public void writeFileHandle(final String baseDir, final String relativeFile, final long sizeOfFile, final FileHandle fileHandle,
                                final LongConsumer bytesWrittenConsumer)
    {
        getLogger().info("write fileHandle: {}/{}, sizeOfFile={}", baseDir, relativeFile, sizeOfFile);

        Path path = Paths.get(baseDir, relativeFile);
        Path parentPath = path.getParent();

        try
        {
            if (Files.notExists(parentPath))
            {
                Files.createDirectories(parentPath);
            }

            if (Files.notExists(path))
            {
                Files.createFile(path);
            }

            try (FileChannel fileChannelReceiver =
                    FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
            {
                MonitoringWritableByteChannel monitoringWritableByteChannel =
                        new MonitoringWritableByteChannel(fileChannelReceiver, bytesWrittenConsumer, false);

                if (fileHandle.getReadableByteChannel() instanceof FileChannel)
                {
                    // Lokales Kopieren
                    FileChannel fileChannelSender = (FileChannel) fileHandle.getReadableByteChannel();

                    // original - apparently has trouble copying large files on Windows
                    // fileChannelSender.transferTo(0, fileChannelSender.size(), resourceReceiver.writableChannel());

                    // Magic number for Windows: (64Mb - 32Kb)
                    // Größere Blöcke kann Windows nicht kopieren, sonst gibs Fehler.
                    long maxWindowsBlockSize = (64L * 1024 * 1024) - (32L * 1024);

                    long count = sizeOfFile;

                    if ((count > maxWindowsBlockSize) && JSyncUtils.isWindows())
                    {
                        count = maxWindowsBlockSize;
                    }

                    long position = 0;

                    while (position < sizeOfFile)
                    {
                        if ((sizeOfFile - position) < count)
                        {
                            count = sizeOfFile - position;
                        }

                        long transfered = fileChannelSender.transferTo(position, count, monitoringWritableByteChannel);

                        position += transfered;
                    }
                }
                else if (fileHandle.getReadableByteChannel() != null)
                {
                    DataBuffer dataBuffer = JSyncUtils.getDataBufferFactory().allocateBuffer(Options.DATABUFFER_SIZE);
                    ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());
                    long totalRead = 0;

                    while (totalRead < sizeOfFile)
                    {
                        byteBuffer.clear();

                        totalRead += fileHandle.getReadableByteChannel().read(byteBuffer);
                        byteBuffer.flip();

                        while (byteBuffer.hasRemaining())
                        {
                            monitoringWritableByteChannel.write(byteBuffer);
                        }
                    }

                    DataBufferUtils.release(dataBuffer);
                }
                else if (fileHandle.getFluxDataBuffer() != null)
                {
                    DataBufferUtils.write(fileHandle.getFluxDataBuffer(), monitoringWritableByteChannel).blockLast();
                }
                else if (fileHandle.getFluxByteBuffer() != null)
                {
                    // @formatter:off
                    fileHandle.getFluxByteBuffer()
                        //.publishOn(Schedulers.elastic()) // Siehe JavaDoc von Schedulers
                        .doOnNext(byteBuffer -> {
                            try
                            {
                                monitoringWritableByteChannel.write(byteBuffer);
                            }
                            catch (IOException ex)
                            {
                                throw new UncheckedIOException(ex);
                            }
                        })
                        .subscribe()
                        .dispose();
                        ;
                    // @formatter:on
                }

                fileChannelReceiver.force(false);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
