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
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.remote.RemoteUtils;
import de.freese.jsync.utils.buffer.DefaultPooledDataBufferFactory;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public interface RemoteSupport
{
    /**
     *
     */
    public static final DataBufferFactory DATA_BUFFER_FACTORY = new DefaultPooledDataBufferFactory(true);

    /**
     * @param channel {@link SocketChannel}
     */
    public default void connect(final SocketChannel channel)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.CONNECT);

            buffer.flip();
            write(channel, buffer);

            long contentLength = readResponseHeader(channel);
            readResponseBody(buffer, channel, contentLength);

            // String message = getSerializer().readFrom(buffer, String.class);
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
            ByteBufferPool.getInstance().release(buffer);
        }
    }
    // /**
    // * @param channel {@link SocketChannel}
    // */
    // public default void connect(final SocketChannel channel)
    // {
    // DataBuffer buffer = DATA_BUFFER_FACTORY.allocateBuffer();
    //
    // try
    // {
    // getSerializerDataBuffer().writeTo(buffer, JSyncCommand.CONNECT);
    //
    // write(channel, buffer);
    //
    // buffer.readPosition(0);
    // buffer.writePosition(0);
    //
    // long contentLength = readResponseHeader(channel);
    // readResponseBody(buffer, channel, contentLength);
    //
    // // String message = getSerializer().readFrom(buffer, String.class);
    // // System.out.println(message);
    // }
    // catch (RuntimeException rex)
    // {
    // throw rex;
    // }
    // catch (IOException ex)
    // {
    // throw new UncheckedIOException(ex);
    // }
    // catch (Exception ex)
    // {
    // throw new RuntimeException(ex);
    // }
    // finally
    // {
    // // DataBufferUtils.release(buffer);
    // ((PooledDataBuffer) buffer).release();
    // }
    // }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativePath String
     */
    public default void createDirectory(final SocketChannel channel, final String baseDir, final String relativePath)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativePath);

            buffer.flip();
            write(channel, buffer);

            readResponseHeader(channel);
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
            ByteBufferPool.getInstance().release(buffer);
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_DELETE);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativePath);
            getSerializer().writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            readResponseHeader(channel);
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
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param logger {@link Logger}
     */
    public default void disconnect(final SocketChannel channel, final Logger logger)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.DISCONNECT);

            buffer.flip();
            write(channel, buffer);

            long contentLength = readResponseHeader(channel);
            readResponseBody(buffer, channel, contentLength);

            // String message = getSerializer().readFrom(buffer, String.class);
            // System.out.println(message);
        }
        catch (Exception ex)
        {
            logger.error(null, ex);
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            long contentLength = readResponseHeader(channel);
            readResponseBody(buffer, channel, contentLength);

            @SuppressWarnings("unused")
            int itemCount = buffer.getInt();

            while (buffer.hasRemaining())
            {
                SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);
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
            ByteBufferPool.getInstance().release(buffer);
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);

            buffer.flip();
            write(channel, buffer);

            long contentLength = readResponseHeader(channel);
            readResponseBody(buffer, channel, contentLength);

            String checksum = getSerializer().readFrom(buffer, String.class);

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
            ByteBufferPool.getInstance().release(buffer);
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(sizeOfFile);

            buffer.flip();
            write(channel, buffer);
            buffer.clear();

            readResponseHeader(channel);

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
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @return {@link Serializer}
     */
    public Serializer<ByteBuffer> getSerializer();

    // /**
    // * @return {@link Serializer}
    // */
    // public Serializer<DataBuffer> getSerializerDataBuffer();

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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(sizeOfFile);

            buffer.flip();
            write(channel, buffer);

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
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param buffer {@link ByteBuffer}
     */
    public default void readChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
                                  final ByteBuffer buffer)
    {
        ByteBuffer bufferResponse = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_READ_CHUNK);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(position);
            buffer.putLong(sizeOfChunk);

            buffer.flip();
            write(channel, buffer);
            buffer.clear();

            // Nur den Status auslesen.
            long contentLength = readResponseHeader(channel);
            readResponseBody(buffer, channel, contentLength);

            // Fertig lesen des Bodys.
            // while (buffer.position() < size)
            // {
            // channelReader.read(buffer);
            // }
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
            ByteBufferPool.getInstance().release(bufferResponse);
        }
    }

    /**
     * Fertig lesen des Bodys.
     *
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     * @throws Exception Falls was schief geht.
     */
    public default void readResponseBody(final ByteBuffer buffer, final SocketChannel channel, final long contentLength) throws Exception
    {
        buffer.clear();

        int totalRead = channel.read(buffer);

        while (totalRead < contentLength)
        {
            totalRead += channel.read(buffer);
        }

        buffer.flip();
    }

    /**
     * Fertig lesen des Bodys.
     *
     * @param buffer {@link DataBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     * @throws Exception Falls was schief geht.
     */
    public default void readResponseBody(final DataBuffer buffer, final SocketChannel channel, final long contentLength) throws Exception
    {
        // ByteBuffer bufferResponse = buffer.asByteBuffer(0, buffer.capacity());
        ByteBuffer bufferResponse = ByteBufferPool.getInstance().get();

        try
        {
            bufferResponse.clear();
            int totalRead = channel.read(bufferResponse);
            bufferResponse.flip();

            buffer.write(bufferResponse);

            while (totalRead < contentLength)
            {
                bufferResponse.clear();
                totalRead += channel.read(bufferResponse);
                bufferResponse.flip();

                buffer.write(bufferResponse);
            }
        }
        catch (Exception ex)
        {
            ByteBufferPool.getInstance().release(bufferResponse);
        }
    }

    /**
     * Einlesen des Headers und ggf. der Exception.
     *
     * @param channel {@link SocketChannel}
     * @return long
     * @throws Exception Falls was schief geht.
     */
    public default long readResponseHeader(final SocketChannel channel) throws Exception
    {
        // Auf keinen Fall mehr lesen als den Header.
        ByteBuffer bufferHeader = ByteBuffer.allocate(12);

        channel.read(bufferHeader);
        bufferHeader.flip();

        int status = bufferHeader.getInt();
        long contentLength = bufferHeader.getLong();

        if (RemoteUtils.STATUS_ERROR == status)
        {
            ByteBuffer bufferException = ByteBufferPool.getInstance().get();

            try
            {
                bufferException.clear();

                // Exception einlesen.
                int totalRead = channel.read(bufferException);

                while (totalRead < contentLength)
                {
                    totalRead += channel.read(bufferException);
                }

                bufferException.flip();

                Exception exception = getSerializer().readFrom(bufferException, Exception.class);

                throw exception;
            }
            finally
            {
                ByteBufferPool.getInstance().release(bufferException);
            }
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_UPDATE);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, syncItem);

            buffer.flip();
            write(channel, buffer);

            readResponseHeader(channel);
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
            ByteBufferPool.getInstance().release(buffer);
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
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_VALIDATE_FILE);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, syncItem);
            getSerializer().writeTo(buffer, withChecksum);

            buffer.flip();
            write(channel, buffer);

            readResponseHeader(channel);
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
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @param channel {@link AsynchronousSocketChannel}
     * @param buffer {@link ByteBuffer}
     * @return int, Bytes written
     * @throws Exception Falls was schief geht.
     */
    default int write(final AsynchronousSocketChannel channel, final ByteBuffer buffer) throws Exception
    {
        int totalWritten = 0;

        while (buffer.hasRemaining())
        {
            totalWritten += channel.write(buffer).get();
        }

        return totalWritten;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     * @return int, Bytes written
     */
    public default int write(final SocketChannel channel, final ByteBuffer buffer) throws Exception
    {
        int totalWritten = 0;

        while (buffer.hasRemaining())
        {
            totalWritten += channel.write(buffer);
        }

        return totalWritten;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link DataBuffer}
     * @throws Exception Falls was schief geht.
     * @return int, Bytes written
     */
    public default int write(final SocketChannel channel, final DataBuffer buffer) throws Exception
    {
        return write(channel, buffer.asByteBuffer());
    }

    /**
     * @param channel {@link SocketChannel}
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param buffer {@link ByteBuffer}
     */
    public default void writeChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
                                   final ByteBuffer buffer)
    {
        ByteBuffer bufferCmd = ByteBufferPool.getInstance().get();

        try
        {
            bufferCmd.clear();
            getSerializer().writeTo(bufferCmd, JSyncCommand.TARGET_WRITE_CHUNK);
            getSerializer().writeTo(bufferCmd, baseDir);
            getSerializer().writeTo(bufferCmd, relativeFile);
            bufferCmd.putLong(position);
            bufferCmd.putLong(sizeOfChunk);

            bufferCmd.flip();

            if (buffer.position() > 0)
            {
                buffer.flip();
            }

            // channelWriter.write(bufferCmd, buffer);
            ByteBuffer[] buffers = new ByteBuffer[]
            {
                    bufferCmd, buffer
            };
            channel.write(buffers);

            readResponseHeader(channel);
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
            ByteBufferPool.getInstance().release(bufferCmd);
        }
    }
}
