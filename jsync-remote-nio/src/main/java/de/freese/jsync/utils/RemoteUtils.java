// Created: 04.09.20
package de.freese.jsync.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import de.freese.jsync.utils.io.SharedByteArrayOutputStream;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public final class RemoteUtils
{
    /**
     *
     */
    private static final byte[] EOL = new byte[]
    {
            0x45, 0x4F, 0x4C
    };

    /**
     * @return int
     */
    public static int getLengthOfEOL()
    {
        return EOL.length;
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @return boolean
     */
    public static boolean isEOL(final ByteBuffer buffer)
    {
        if ((buffer.limit() - buffer.position()) < 3)
        {
            // Buffer hat keine 3 Bytes mehr.
            return false;
        }

        int index = buffer.position();

        byte e = buffer.get(index);
        byte o = buffer.get(index + 1);
        byte l = buffer.get(index + 2);

        return (EOL[0] == e) && (EOL[1] == o) && (EOL[2] == l);
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @return boolean
     */
    public static boolean isResponseOK(final ByteBuffer buffer)
    {
        if ((buffer.limit() - buffer.position()) < 4)
        {
            // Buffer hat keine 4 Bytes mehr.
            return false;
        }

        return buffer.getInt() == 200;
    }

    /**
     * Wegen Chunked-Data den Response erst mal sammeln.
     *
     * @param channel {@link AsynchronousSocketChannel}
     * @return {@link ByteBuffer}
     * @throws IOException Falls was schief geht.
     * @throws ExecutionException Falls was schief geht.
     * @throws InterruptedException Falls was schief geht.
     */
    public static ByteBuffer readUntilEOL(final AsynchronousSocketChannel channel) throws IOException, InterruptedException, ExecutionException
    {
        SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

        ByteBuffer buffer = ByteBufferPool.getInstance().get();
        buffer.clear();

        Future<Integer> futureResponse = channel.read(buffer);

        try
        {
            while (futureResponse.get() > 0)
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
                futureResponse = channel.read(buffer);
            }
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }

        ByteBuffer bufferData = sbaos.toByteBuffer();

        return bufferData;
    }

    /**
     * Wegen Chunked-Data den Response erst mal sammeln.
     *
     * @param channel {@link SocketChannel}
     * @return {@link ByteBuffer}
     * @throws IOException Falls was schief geht.
     */
    public static ByteBuffer readUntilEOL(final SocketChannel channel) throws IOException
    {
        SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

        ByteBuffer buffer = ByteBufferPool.getInstance().get();
        buffer.clear();

        try
        {
            while (channel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.remaining() > RemoteUtils.getLengthOfEOL())
                {
                    sbaos.write(buffer, buffer.remaining() - RemoteUtils.getLengthOfEOL());
                }

                if (RemoteUtils.isEOL(buffer))
                {
                    break;
                }

                sbaos.write(buffer, buffer.remaining());

                buffer.clear();
            }
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }

        ByteBuffer bufferData = sbaos.toByteBuffer();

        return bufferData;
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    public static void writeEOL(final ByteBuffer buffer)
    {
        buffer.put(EOL);
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    public static void writeResponseERROR(final ByteBuffer buffer)
    {
        buffer.putInt(500);
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    public static void writeResponseOK(final ByteBuffer buffer)
    {
        buffer.putInt(200);
    }

    /**
     * Erzeugt eine neue Instanz von {@link RemoteUtils}
     */
    private RemoteUtils()
    {
        super();
    }
}
