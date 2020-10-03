// Created: 06.09.2020
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public interface RemoteSupport
{
    /**
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void connect(final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.CONNECT);

            buffer.flip();
            channelWriter.write(buffer);

            long contentLength = readResponseHeader(channelReader);
            readResponseBody(buffer, channelReader, contentLength);

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

    /**
     * @param baseDir String
     * @param relativePath String
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void createDirectory(final String baseDir, final String relativePath, final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativePath);

            buffer.flip();
            channelWriter.write(buffer);

            readResponseHeader(channelReader);
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
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void delete(final String baseDir, final String relativePath, final boolean followSymLinks, final ChannelWriter channelWriter,
                               final ChannelReader channelReader)
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
            channelWriter.write(buffer);

            readResponseHeader(channelReader);
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
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     * @param logger {@link Logger}
     */
    public default void disconnect(final ChannelWriter channelWriter, final ChannelReader channelReader, final Logger logger)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.DISCONNECT);

            buffer.flip();
            channelWriter.write(buffer);

            long contentLength = readResponseHeader(channelReader);
            readResponseBody(buffer, channelReader, contentLength);

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
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumerSyncItem {@link Consumer}
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem,
                                          final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, followSymLinks);

            buffer.flip();
            channelWriter.write(buffer);

            long contentLength = readResponseHeader(channelReader);
            readResponseBody(buffer, channelReader, contentLength);

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
     * @param baseDir String
     * @param relativeFile String
     * @param consumerBytesRead {@link LongConsumer}
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     * @return String
     */
    public default String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead, final ChannelWriter channelWriter,
                                      final ChannelReader channelReader)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);

            buffer.flip();
            channelWriter.write(buffer);

            long contentLength = readResponseHeader(channelReader);
            readResponseBody(buffer, channelReader, contentLength);

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
     * @param <C> Type
     * @param baseDir String
     * @param relativeFile String
     * @param size long
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     * @param channelSupplier {@link Supplier}
     * @param channelReleaser {@link Consumer}
     * @return {@link ReadableByteChannel}
     */
    public default <C extends NetworkChannel> ReadableByteChannel getReadableChannel(final String baseDir, final String relativeFile, final long size,
                                                                                     final ChannelWriter channelWriter, final ChannelReader channelReader,
                                                                                     final Supplier<C> channelSupplier, final Consumer<C> channelReleaser)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(size);

            buffer.flip();
            channelWriter.write(buffer);
            buffer.clear();

            readResponseHeader(channelReader);

            return new NoCloseReadableByteChannel<>(channelSupplier.get(), channelReader, channelReleaser);
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

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param size long
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     * @param channelSupplier {@link Supplier}
     * @param channelReleaser {@link Consumer}
     * @return {@link WritableByteChannel}
     */
    public default <C extends NetworkChannel> WritableByteChannel getWritableChannel(final String baseDir, final String relativeFile, final long size,
                                                                                     final ChannelWriter channelWriter, final ChannelReader channelReader,
                                                                                     final Supplier<C> channelSupplier, final Consumer<C> channelReleaser)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(size);

            buffer.flip();
            channelWriter.write(buffer);

            // Response auslesen erfolgt in NoCloseWritableByteChannel#close.
            return new NoCloseWritableByteChannel<>(channelSupplier.get(), channelWriter, channelReader, channelReleaser);
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
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @param buffer {@link ByteBuffer}
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer,
                                  final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer bufferResponse = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.READ_CHUNK);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, relativeFile);
            buffer.putLong(position);
            buffer.putLong(size);

            buffer.flip();
            channelWriter.write(buffer);
            buffer.clear();

            // Nur den Status auslesen.
            long contentLength = readResponseHeader(channelReader);
            readResponseBody(buffer, channelReader, contentLength);

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
     * @param channelReader {@link ChannelReader}
     * @param contentLength long
     * @throws Exception Falls was schief geht.
     */
    public default void readResponseBody(final ByteBuffer buffer, final ChannelReader channelReader, final long contentLength) throws Exception
    {
        buffer.clear();

        int totalRead = channelReader.read(buffer);

        while (totalRead < contentLength)
        {
            totalRead += channelReader.read(buffer);
        }

        buffer.flip();
    }

    /**
     * Einlesen des Headers und ggf. der Exception.
     *
     * @param channelReader {@link ChannelReader}
     * @return long
     * @throws Exception Falls was schief geht.
     */
    public default long readResponseHeader(final ChannelReader channelReader) throws Exception
    {
        // Auf keinen Fall mehr lesen als den Header.
        ByteBuffer bufferHeader = ByteBuffer.allocate(12);

        channelReader.read(bufferHeader);
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
                int totalRead = channelReader.read(bufferException);

                while (totalRead < contentLength)
                {
                    totalRead += channelReader.read(bufferException);
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
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void update(final String baseDir, final SyncItem syncItem, final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            getSerializer().writeTo(buffer, JSyncCommand.TARGET_UPDATE);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, syncItem);

            buffer.flip();
            channelWriter.write(buffer);

            readResponseHeader(channelReader);
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
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final ChannelWriter channelWriter,
                                     final ChannelReader channelReader)
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
            channelWriter.write(buffer);

            readResponseHeader(channelReader);
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
    public default int write(final AsynchronousSocketChannel channel, final ByteBuffer buffer) throws Exception
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
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @param buffer {@link ByteBuffer}
     * @param channelWriter {@link ChannelWriter}
     * @param channelReader {@link ChannelReader}
     */
    public default void writeChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer,
                                   final ChannelWriter channelWriter, final ChannelReader channelReader)
    {
        ByteBuffer bufferCmd = ByteBufferPool.getInstance().get();

        try
        {
            bufferCmd.clear();
            getSerializer().writeTo(bufferCmd, JSyncCommand.WRITE_CHUNK);
            getSerializer().writeTo(bufferCmd, baseDir);
            getSerializer().writeTo(bufferCmd, relativeFile);
            bufferCmd.putLong(position);
            bufferCmd.putLong(size);

            bufferCmd.flip();

            if (buffer.position() > 0)
            {
                buffer.flip();
            }

            // channelWriter.write(bufferCmd, buffer);
            channelWriter.write(bufferCmd);
            channelWriter.write(buffer);

            readResponseHeader(channelReader);
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
