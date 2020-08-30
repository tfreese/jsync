// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.JSyncUtils;

/**
 * Verarbeitet den Request und Response.<br>
 * Sync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see IoHandler
 */
@SuppressWarnings("resource")
public class JSyncIoHandler implements IoHandler
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncIoHandler.class);
    /**
     *
     */
    private static final ThreadLocal<ByteBuffer> THREAD_LOCAL_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(Options.BUFFER_SIZE));
    /**
     *
     */
    private static final ThreadLocal<Receiver> THREAD_LOCAL_RECEIVER = ThreadLocal.withInitial(LocalhostReceiver::new);
    /**
     *
     */
    private static final ThreadLocal<Sender> THREAD_LOCAL_SENDER = ThreadLocal.withInitial(LocalhostSender::new);

    /**
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    private static void writeBuffer(final SelectionKey selectionKey, final ByteBuffer buffer) throws IOException
    {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
    }

    /**
     * Erstellt ein neues {@link JSyncIoHandler} Object.
     */
    public JSyncIoHandler()
    {
        super();
    }

    /**
     * @see IoHandler#read(SelectionKey)
     */
    @Override
    public void read(final SelectionKey selectionKey) throws Exception
    {
        ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

        // JSyncCommand lesen.
        ByteBuffer buffer = THREAD_LOCAL_BUFFER.get();

        buffer.clear();

        int bytesRead = channel.read(buffer);

        if (bytesRead == -1)
        {
            // Nach Disconnect
            return;
        }

        buffer.flip();

        JSyncCommand command = Serializers.readFrom(buffer, JSyncCommand.class);
        getLogger().debug("{}: read command: {}", getRemoteAddress(selectionKey), command);

        if (command == null)
        {
            getLogger().error("unknown JSyncCommand");
            selectionKey.interestOps(SelectionKey.OP_READ);
            return;
        }

        switch (command)
        {
            case DISCONNECT:
                selectionKey.attach(null);
                // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                selectionKey.cancel();
                break;

            case CONNECT:
                // Empty
                break;

            case TARGET_CHECKSUM:
                createChecksum(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_CREATE_SYNC_ITEMS:
                createSyncItems(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_DELETE:
                delete(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_WRITEABLE_FILE_CHANNEL:
                fileChannel(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_VALIDATE_FILE:
                validate(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_UPDATE:
                update(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case SOURCE_CHECKSUM:
                createChecksum(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                createSyncItems(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                break;

            case SOURCE_READABLE_FILE_CHANNEL:
                fileChannel(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                break;

            default:
                break;
        }

        // selectionKey.interestOps(SelectionKey.OP_READ);
        // selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @see IoHandler#write(SelectionKey)
     */
    @Override
    public void write(final SelectionKey selectionKey) throws Exception
    {
        // WritableByteChannel channel = (WritableByteChannel) selectionKey.channel();

        if (selectionKey.attachment() instanceof Runnable)
        {
            Runnable task = (Runnable) selectionKey.attachment();
            task.run();
        }

        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param selectionKey SelectionKey
     * @return String
     */
    private String getRemoteAddress(final SelectionKey selectionKey)
    {
        try
        {
            return ((SocketChannel) selectionKey.channel()).getRemoteAddress().toString();
        }
        catch (IOException ex)
        {
            return "";
        }
    }

    /**
     * Create the checksum.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param fileSystem {@link FileSystem}
     * @throws Exception Falls was schief geht.
     */
    protected void createChecksum(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        getLogger().debug("{}: Create Checksum: {}/{}", getRemoteAddress(selectionKey), baseDir, relativeFile);

        String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
        });

        getLogger().debug("{}: Checksum created: {}/{}", getRemoteAddress(selectionKey), baseDir, relativeFile);

        buffer.clear();
        Serializers.writeTo(buffer, checksum);
        buffer.flip();

        writeBuffer(selectionKey, buffer);
    }

    /**
     * Create the Sync-Items.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param fileSystem {@link FileSystem}
     * @throws Exception Falls was schief geht.
     */
    protected void createSyncItems(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        boolean followSymLinks = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("{}: Create SyncItems: {}", getRemoteAddress(selectionKey), baseDir);

        fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("{}: Send SyncItem: {}", getRemoteAddress(selectionKey), syncItem);

            buffer.clear();
            Serializers.writeTo(buffer, syncItem);
            buffer.flip();

            try
            {
                writeBuffer(selectionKey, buffer);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        });

        // EOL
        buffer.clear();
        Serializers.writeEOL(buffer);
        buffer.flip();

        writeBuffer(selectionKey, buffer);

        getLogger().debug("{}: SyncItems written: {}", getRemoteAddress(selectionKey), baseDir);
    }

    /**
     * Delete Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void delete(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativePath = Serializers.readFrom(buffer, String.class);
        boolean followSymLinks = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("{}: Delete: {}/{}", getRemoteAddress(selectionKey), baseDir, relativePath);

        receiver.delete(baseDir, relativePath, followSymLinks);

        getLogger().debug("{}: Deleted: {}/{}", getRemoteAddress(selectionKey), baseDir, relativePath);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void fileChannel(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        getLogger().debug("{}: receive File {}/{}", getRemoteAddress(selectionKey), baseDir, relativeFile);

        ReadableByteChannel inChannel = (ReadableByteChannel) selectionKey.channel();

        long bytesRead = 0;
        long bytesWrote = 0;

        try (WritableByteChannel outChannel = receiver.getChannel(baseDir, relativeFile))
        {
            // Beim Empfang sind weitere Daten im Buffer enthalten.
            do
            {
                while (buffer.hasRemaining())
                {
                    bytesWrote += outChannel.write(buffer);
                }

                buffer.clear();
                bytesRead = inChannel.read(buffer);
                buffer.flip();
            }
            while (bytesRead > 0);
            // while (inChannel.read(buffer) > 0)
            // {
            // buffer.flip();
            //
            // while (buffer.hasRemaining())
            // {
            // bytesWrote += outChannel.write(buffer);
            // }
            //
            // buffer.clear();
            // }

            if (outChannel instanceof FileChannel)
            {
                ((FileChannel) outChannel).force(true);
            }
        }

        getLogger().debug("{}: file received {}/{}; bytesWrote = {}", getRemoteAddress(selectionKey), baseDir, relativeFile,
                JSyncUtils.toHumanReadableSize(bytesWrote));
    }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     * @throws Exception Falls was schief geht.
     */
    protected void fileChannel(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        getLogger().debug("{}: send File {}/{}", getRemoteAddress(selectionKey), baseDir, relativeFile);

        WritableByteChannel outChannel = (WritableByteChannel) selectionKey.channel();

        long bytesWrote = 0;

        buffer.clear();

        try (ReadableByteChannel inChannel = sender.getChannel(baseDir, relativeFile))
        {
            while (inChannel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    bytesWrote += outChannel.write(buffer);
                }

                buffer.clear();
            }
        }

        getLogger().debug("{}: file send {}/{}; bytesWrote = {}", getRemoteAddress(selectionKey), baseDir, relativeFile,
                JSyncUtils.toHumanReadableSize(bytesWrote));
    }

    /**
     * Update Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void update(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);

        getLogger().debug("{}: Update: {}/{}", getRemoteAddress(selectionKey), baseDir, syncItem.getRelativePath());

        receiver.update(baseDir, syncItem);

        getLogger().debug("{}: Updated: {}/{}", getRemoteAddress(selectionKey), baseDir, syncItem.getRelativePath());
    }

    /**
     * Validate Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void validate(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
        boolean withChecksum = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("{}: Validate: {}/{}", getRemoteAddress(selectionKey), baseDir, syncItem.getRelativePath());

        try
        {
            receiver.validateFile(baseDir, syncItem, withChecksum);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            buffer.clear();
            Serializers.writeTo(buffer, ex.getMessage());
            buffer.flip();

            writeBuffer(selectionKey, buffer);
        }

        getLogger().debug("{}: Validated: {}/{}", getRemoteAddress(selectionKey), baseDir, syncItem.getRelativePath());
    }
}
