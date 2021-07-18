// Created: 21.10.2020
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;

import io.rsocket.Payload;

/**
 * @author Thomas Freese
 */
public final class RSocketUtils
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketUtils.class);

    /**
     * Release the given data buffer, if it is a {@link PooledDataBuffer} and has been {@linkplain PooledDataBuffer#isAllocated() allocated}.
     *
     * @param dataBuffer the data buffer to release
     *
     * @return {@code true} if the buffer was released; {@code false} otherwise.
     */
    public static boolean release(@Nullable final DataBuffer dataBuffer)
    {
        if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer)
        {
            if (pooledDataBuffer.isAllocated())
            {
                try
                {
                    return pooledDataBuffer.release();
                }
                catch (IllegalStateException ex)
                {
                    // Avoid dependency on Netty: IllegalReferenceCountException
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
                    }
                }
            }
        }

        return false;
    }

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
