// Created: 21.10.2020
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import io.rsocket.Payload;

/**
 * @author Thomas Freese
 */
public final class RSocketUtils
{
    /**
     * @param payload {@link Payload}
     */
    public static void release(final Payload payload)
    {
        if (payload == null)
        {
            return;
        }

        if (payload.refCnt() > 2)
        {
            payload.release();
        }
        else
        {
            payload.retain();
        }
    }

    /**
     * @param payload {@link Payload}
     * @param channel {@link WritableByteChannel}
     *
     * @return int
     */
    public static int write(final Payload payload, final WritableByteChannel channel)
    {
        ByteBuffer byteBuffer = payload.getData();

        int bytesWritten = 0;

        try
        {
            while (byteBuffer.hasRemaining())
            {
                bytesWritten += channel.write(byteBuffer);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        return bytesWritten;
    }

    /**
     * Erstellt ein neues {@link RSocketUtils} Object.
     */
    private RSocketUtils()
    {
        super();
    }
}
