// Created: 06.09.2020
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.remote.RemoteUtils;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.buffer.DataBufferAdapter;

/**
 * @author Thomas Freese
 */
public interface RemoteSupport
{
    /**
     *
     */
    static final DataBufferFactory DATA_BUFFER_FACTORY = JSyncUtils.getDataBufferFactory();

    /**
    *
    */
    static final Serializer<DataBuffer> SERIALIZER = DefaultSerializer.of(new DataBufferAdapter());

    /**
     * @param channel {@link SocketChannel}
     */
    public default void connect(final SocketChannel channel)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.CONNECT);

            write(channel, dataBuffer);

            long contentLength = readResponseHeader(dataBuffer, channel);
            readResponseBody(dataBuffer, channel, contentLength);

            // String message = getSerializerDataBuffer().readFrom(dataBuffer, String.class);
            // System.out.println(message);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativePath String
     */
    public default void createDirectory(final SocketChannel channel, final String baseDir, final String relativePath)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_CREATE_DIRECTORY);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativePath);

            write(channel, dataBuffer);

            readResponseHeader(dataBuffer, channel);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     */
    public default void delete(final SocketChannel channel, final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_DELETE);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativePath);
            getSerializer().writeTo(dataBuffer, followSymLinks);

            write(channel, dataBuffer);

            readResponseHeader(dataBuffer, channel);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param logger {@link Logger}
     */
    public default void disconnect(final SocketChannel channel, final Logger logger)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.DISCONNECT);

            write(channel, dataBuffer);

            long contentLength = readResponseHeader(dataBuffer, channel);
            readResponseBody(dataBuffer, channel, contentLength);

            // String message = getSerializerDataBuffer().readFrom(buffer, String.class);
            // System.out.println(message);
        }
        catch (Exception ex)
        {
            logger.error(null, ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumerSyncItem {@link Consumer}
     */
    public default void generateSyncItems(final SocketChannel channel, final String baseDir, final boolean followSymLinks,
                                          final Consumer<SyncItem> consumerSyncItem)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, followSymLinks);

            write(channel, dataBuffer);

            long contentLength = readResponseHeader(dataBuffer, channel);
            readResponseBody(dataBuffer, channel, contentLength);

            @SuppressWarnings("unused")
            int itemCount = getSerializer().readFrom(dataBuffer, int.class);

            while (dataBuffer.readableByteCount() > 0)
            {
                SyncItem syncItem = getSerializer().readFrom(dataBuffer, SyncItem.class);
                consumerSyncItem.accept(syncItem);
            }
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativeFile String
     * @param consumerBytesRead {@link LongConsumer}
     * @return String
     */
    public default String getChecksum(final SocketChannel channel, final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.SOURCE_CHECKSUM);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativeFile);

            write(channel, dataBuffer);

            long contentLength = readResponseHeader(dataBuffer, channel);
            readResponseBody(dataBuffer, channel, contentLength);

            String checksum = getSerializer().readFrom(dataBuffer, String.class);

            return checksum;
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param channelReleaser {@link Consumer}
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @return {@link ReadableByteChannel}
     */
    public default ReadableByteChannel getReadableChannel(final SocketChannel channel, final Consumer<SocketChannel> channelReleaser, final String baseDir,
                                                          final String relativeFile, final long sizeOfFile)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.SOURCE_READABLE_RESOURCE);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativeFile);
            getSerializer().writeTo(dataBuffer, sizeOfFile);

            write(channel, dataBuffer);

            readResponseHeader(dataBuffer, channel);

            return new NoCloseReadableByteChannel(channel, channelReleaser);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @return {@link Serializer}
     */
    public default Serializer<DataBuffer> getSerializer()
    {
        return SERIALIZER;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param channelReleaser {@link Consumer}
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @return {@link WritableByteChannel}
     */
    public default WritableByteChannel getWritableChannel(final SocketChannel channel, final Consumer<SocketChannel> channelReleaser, final String baseDir,
                                                          final String relativeFile, final long sizeOfFile)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_WRITEABLE_RESOURCE);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativeFile);
            getSerializer().writeTo(dataBuffer, sizeOfFile);

            write(channel, dataBuffer);

            // Response auslesen erfolgt in NoCloseWritableByteChannel#close.
            return new NoCloseWritableByteChannel(channel, channelReleaser);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param byteBufferChunk {@link ByteBuffer}
     */
    public default void readChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
                                  final ByteBuffer byteBufferChunk)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.SOURCE_READ_CHUNK);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativeFile);
            getSerializer().writeTo(dataBuffer, position);
            getSerializer().writeTo(dataBuffer, sizeOfChunk);

            write(channel, dataBuffer);

            // Nur den Status auslesen.
            long contentLength = readResponseHeader(dataBuffer, channel);
            readResponseBody(byteBufferChunk, channel, contentLength);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * Fertig lesen des Bodys.
     *
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     * @return int
     * @throws Exception Falls was schief geht.
     */
    public default int readResponseBody(final ByteBuffer buffer, final SocketChannel channel, final long contentLength) throws Exception
    {
        buffer.clear();

        int totalRead = channel.read(buffer);

        while (totalRead < contentLength)
        {
            totalRead += channel.read(buffer);
        }

        buffer.flip();

        return totalRead;
    }

    /**
     * Fertig lesen des Bodys.
     *
     * @param dataBuffer {@link DataBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     * @throws Exception Falls was schief geht.
     */
    public default void readResponseBody(final DataBuffer dataBuffer, final SocketChannel channel, final long contentLength) throws Exception
    {
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());

        int totalRead = readResponseBody(byteBuffer, channel, contentLength);
        dataBuffer.writePosition(totalRead);
    }

    /**
     * Einlesen des Headers und ggf. der Exception.
     *
     * @param dataBuffer {@link DataBuffer}
     * @param channel {@link SocketChannel}
     * @return long
     * @throws Exception Falls was schief geht.
     */
    public default long readResponseHeader(final DataBuffer dataBuffer, final SocketChannel channel) throws Exception
    {
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        // Auf keinen Fall mehr lesen als den Header: 12 Bytes.
        ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 12);

        channel.read(byteBuffer);
        dataBuffer.writePosition(12);

        int status = getSerializer().readFrom(dataBuffer, int.class);
        long contentLength = getSerializer().readFrom(dataBuffer, long.class);

        if (RemoteUtils.STATUS_ERROR == status)
        {
            byteBuffer = dataBuffer.asByteBuffer(12, (int) contentLength);

            // Exception einlesen.
            int totalRead = channel.read(byteBuffer);
            dataBuffer.writePosition(dataBuffer.writePosition() + totalRead);

            while (totalRead < contentLength)
            {
                totalRead += channel.read(byteBuffer);
                dataBuffer.writePosition(dataBuffer.writePosition() + totalRead);
            }

            Exception exception = getSerializer().readFrom(dataBuffer, Exception.class);

            throw exception;
        }

        return contentLength;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    public default void update(final SocketChannel channel, final String baseDir, final SyncItem syncItem)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_UPDATE);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, syncItem);

            write(channel, dataBuffer);

            readResponseHeader(dataBuffer, channel);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     */
    public default void validateFile(final SocketChannel channel, final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_VALIDATE_FILE);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, syncItem);
            getSerializer().writeTo(dataBuffer, withChecksum);

            write(channel, dataBuffer);

            readResponseHeader(dataBuffer, channel);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param channel {@link AsynchronousSocketChannel}
     * @param byteBuffer {@link ByteBuffer}
     * @return int, Bytes written
     * @throws Exception Falls was schief geht.
     */
    default int write(final AsynchronousSocketChannel channel, final ByteBuffer byteBuffer) throws Exception
    {
        int totalWritten = 0;

        while (byteBuffer.hasRemaining())
        {
            totalWritten += channel.write(byteBuffer).get();
        }

        return totalWritten;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param byteBuffer {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     * @return int, Bytes written
     */
    public default int write(final SocketChannel channel, final ByteBuffer byteBuffer) throws Exception
    {
        int totalWritten = 0;

        while (byteBuffer.hasRemaining())
        {
            totalWritten += channel.write(byteBuffer);
        }

        return totalWritten;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param dataBuffer {@link DataBuffer}
     * @throws Exception Falls was schief geht.
     * @return int, Bytes written
     */
    public default int write(final SocketChannel channel, final DataBuffer dataBuffer) throws Exception
    {
        return write(channel, dataBuffer.asByteBuffer());
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param byteBufferChunk {@link ByteBuffer}
     */
    public default void writeChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
                                   final ByteBuffer byteBufferChunk)
    {
        DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_WRITE_CHUNK);
            getSerializer().writeTo(dataBuffer, baseDir);
            getSerializer().writeTo(dataBuffer, relativeFile);
            getSerializer().writeTo(dataBuffer, position);
            getSerializer().writeTo(dataBuffer, sizeOfChunk);

            if (byteBufferChunk.position() > 0)
            {
                byteBufferChunk.flip();
            }

            // channelWriter.write(bufferCmd, buffer);
            ByteBuffer[] buffers = new ByteBuffer[]
            {
                    dataBuffer.asByteBuffer(), byteBufferChunk
            };
            channel.write(buffers);

            readResponseHeader(dataBuffer, channel);
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }
}
