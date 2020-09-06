// Created: 06.09.2020
package de.freese.jsync.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.RemoteUtils;
import de.freese.jsync.utils.io.SharedByteArrayOutputStream;
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
            Serializers.writeTo(buffer, JSyncCommand.CONNECT);

            buffer.flip();
            channelWriter.write(buffer);

            ByteBuffer byteBufferResponse = readUntilEOL(buffer, channelReader);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @param buffer {@link ByteBuffer}
     * @param channelReader {@link ChannelReader}
     * @return {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     */
    public default ByteBuffer readUntilEOL(final ByteBuffer buffer, final ChannelReader channelReader) throws Exception
    {
        SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

        buffer.clear();

        while (channelReader.read(buffer) > 0)
        {
            buffer.flip();

            while (buffer.remaining() > RemoteUtils.getLengthOfEOL())
            {
                sbaos.write(buffer, buffer.remaining() - RemoteUtils.getLengthOfEOL());
            }

            if (RemoteUtils.isEOL(buffer))
            {
                buffer.clear();
                break;
            }

            sbaos.write(buffer, buffer.remaining());

            buffer.clear();
        }

        ByteBuffer bufferData = sbaos.toByteBuffer();

        return bufferData;
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
}
