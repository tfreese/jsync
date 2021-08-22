// Created: 06.09.2020
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.slf4j.Logger;

import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.nio.utils.io.NoCloseReadableByteChannel;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public interface RemoteSupport
{
    /**
    *
    */
    Serializer<ByteBuffer> SERIALIZER = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Liefert den {@link FileHandle} zur Datei.
     *
     * @param channel {@link SocketChannel}
     * @param channelReleaser {@link Consumer}
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     *
     * @return {@link ReadableByteChannel}
     */
    default ReadableByteChannel getReadableChannel(final SocketChannel channel, final Consumer<SocketChannel> channelReleaser, final String baseDir,
                                                   final String relativeFile, final long sizeOfFile)
    {
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().obtain();

        try
        {
            getSerializer().writeTo(byteBuffer, JSyncCommand.SOURCE_READ_FILE);
            getSerializer().writeTo(byteBuffer, baseDir);
            getSerializer().writeTo(byteBuffer, relativeFile);
            getSerializer().writeTo(byteBuffer, sizeOfFile);

            write(channel, byteBuffer);

            readResponseHeader(byteBuffer, channel);

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
            ByteBufferPool.getInstance().free(byteBuffer);
        }
    }

    /**
     * @return {@link Serializer}
     */
    default Serializer<ByteBuffer> getSerializer()
    {
        return SERIALIZER;
    }

    // /**
    // * @param channel {@link SocketChannel}
    // * @param channelReleaser {@link Consumer}
    // * @param baseDir String
    // * @param relativeFile String
    // * @param sizeOfFile long
    // * @return {@link WritableByteChannel}
    // */
    // public default WritableByteChannel getWritableChannel(final SocketChannel channel, final Consumer<SocketChannel> channelReleaser, final String baseDir,
    // final String relativeFile, final long sizeOfFile)
    // {
    // DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
    // dataBuffer.readPosition(0).writePosition(0);
    //
    // try
    // {
    // getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_WRITEABLE_RESOURCE);
    // getSerializer().writeTo(dataBuffer, baseDir);
    // getSerializer().writeTo(dataBuffer, relativeFile);
    // getSerializer().writeTo(dataBuffer, sizeOfFile);
    //
    // write(channel, dataBuffer);
    //
    // // Response auslesen erfolgt in NoCloseWritableByteChannel#close.
    // return new NoCloseWritableByteChannel(channel, channelReleaser);
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
    // DataBufferUtils.release(dataBuffer);
    // }
    // }

    // /**
    // * @param channel {@link SocketChannel}
    // * @param baseDir String
    // * @param relativeFile String
    // * @param position long
    // * @param sizeOfChunk long
    // * @param byteBufferChunk {@link ByteBuffer}
    // */
    // public default void readChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
    // final ByteBuffer byteBufferChunk)
    // {
    // DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
    // dataBuffer.readPosition(0).writePosition(0);
    //
    // try
    // {
    // getSerializer().writeTo(dataBuffer, JSyncCommand.SOURCE_READ_CHUNK);
    // getSerializer().writeTo(dataBuffer, baseDir);
    // getSerializer().writeTo(dataBuffer, relativeFile);
    // getSerializer().writeTo(dataBuffer, position);
    // getSerializer().writeTo(dataBuffer, sizeOfChunk);
    //
    // write(channel, dataBuffer);
    //
    // // Nur den Status auslesen.
    // long contentLength = readResponseHeader(dataBuffer, channel);
    // readResponseBody(byteBufferChunk, channel, contentLength);
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
    // DataBufferUtils.release(dataBuffer);
    // }
    // }

    /**
     * Fertig lesen des Bodys.
     *
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     *
     * @return int
     *
     * @throws Exception Falls was schief geht.
     */
    default int readResponseBody(final ByteBuffer buffer, final SocketChannel channel, final long contentLength) throws Exception
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
     * Einlesen des Headers und ggf. der Exception.
     *
     * @param byteBuffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     *
     * @return long
     *
     * @throws Exception Falls was schief geht.
     */
    default long readResponseHeader(final ByteBuffer byteBuffer, final SocketChannel channel) throws Exception
    {
        byteBuffer.clear();

        // Auf keinen Fall mehr lesen als den Header: 12 Bytes
        ByteBuffer byteBufferHeader = byteBuffer.slice(0, 12);

        channel.read(byteBufferHeader);
        byteBufferHeader.flip();

        int status = getSerializer().readFrom(byteBufferHeader, int.class);
        long contentLength = getSerializer().readFrom(byteBufferHeader, long.class);

        byteBuffer.clear();

        if (RemoteUtils.STATUS_ERROR == status)
        {
            // Exception einlesen.
            int totalRead = channel.read(byteBuffer);

            while (totalRead < contentLength)
            {
                int bytesRead = channel.read(byteBuffer);
                totalRead += bytesRead;
            }

            byteBuffer.flip();

            Exception exception = getSerializer().readFrom(byteBuffer, Exception.class);

            throw exception;
        }

        return contentLength;
    }

    /**
     * @param channel {@link AsynchronousSocketChannel}
     * @param byteBuffer {@link ByteBuffer}
     *
     * @return int, Bytes written
     *
     * @throws Exception Falls was schief geht.
     */
    default int write(final AsynchronousSocketChannel channel, final ByteBuffer byteBuffer) throws Exception
    {
        int totalWritten = 0;

        while (byteBuffer.hasRemaining())
        {
            int bytesWritten = channel.write(byteBuffer).get();

            totalWritten += bytesWritten;
        }

        return totalWritten;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param byteBuffer {@link ByteBuffer}
     *
     * @throws Exception Falls was schief geht.
     *
     * @return int, Bytes written
     */
    default int write(final SocketChannel channel, final ByteBuffer byteBuffer) throws Exception
    {
        if (byteBuffer.position() > 0)
        {
            byteBuffer.flip();
        }

        int totalWritten = 0;

        while (byteBuffer.hasRemaining())
        {
            int bytesWritten = channel.write(byteBuffer);

            totalWritten += bytesWritten;
        }

        return totalWritten;
    }

    // /**
    // * @param channel {@link SocketChannel}
    // * @param baseDir String
    // * @param relativeFile String
    // * @param position long
    // * @param sizeOfChunk long
    // * @param byteBufferChunk {@link ByteBuffer}
    // */
    // public default void writeChunk(final SocketChannel channel, final String baseDir, final String relativeFile, final long position, final long sizeOfChunk,
    // final ByteBuffer byteBufferChunk)
    // {
    // DataBuffer dataBuffer = DATA_BUFFER_FACTORY.allocateBuffer();
    // dataBuffer.readPosition(0).writePosition(0);
    //
    // try
    // {
    // getSerializer().writeTo(dataBuffer, JSyncCommand.TARGET_WRITE_CHUNK);
    // getSerializer().writeTo(dataBuffer, baseDir);
    // getSerializer().writeTo(dataBuffer, relativeFile);
    // getSerializer().writeTo(dataBuffer, position);
    // getSerializer().writeTo(dataBuffer, sizeOfChunk);
    //
    // if (byteBufferChunk.position() > 0)
    // {
    // byteBufferChunk.flip();
    // }
    //
    // // channelWriter.write(bufferCmd, buffer);
    // ByteBuffer[] buffers = new ByteBuffer[]
    // {
    // dataBuffer.asByteBuffer(), byteBufferChunk
    // };
    // channel.write(buffers);
    //
    // readResponseHeader(dataBuffer, channel);
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
    // DataBufferUtils.release(dataBuffer);
    // }
    // }

    // /**
    // * Schreibt den {@link FileHandle} in die Datei.
    // *
    // * @param channel {@link SocketChannel}
    // * @param baseDir String
    // * @param relativeFile String
    // * @param sizeOfFile long
    // * @param fileHandle {@link FileHandle}
    // * @param bytesWrittenConsumer {@link LongConsumer}
    // */
    // default void writeFileHandle(final SocketChannel channel, final String baseDir, final String relativeFile, final long sizeOfFile,
    // final FileHandle fileHandle, final LongConsumer bytesWrittenConsumer)
    // {
    // ByteBuffer byteBuffer = ByteBufferPool.getInstance().obtain();
    //
    // try
    // {
    // getSerializer().writeTo(byteBuffer, JSyncCommand.TARGET_WRITE_FILE);
    // getSerializer().writeTo(byteBuffer, baseDir);
    // getSerializer().writeTo(byteBuffer, relativeFile);
    // getSerializer().writeTo(byteBuffer, sizeOfFile);
    //
    // write(channel, byteBuffer);
    //
    // // Ohne diese Pause kann es beim Remote-Transfer Hänger geben.
    // // Warum auch immer ...
    // Thread.sleep(1);
    //
    // fileHandle.writeTo(channel, sizeOfFile);
    //
    // readResponseHeader(byteBuffer, channel);
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
    // ByteBufferPool.getInstance().free(byteBuffer);
    // }
    // }
}
