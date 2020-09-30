// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Thomas Freese
 */
public interface JsyncResponse extends Closeable
{
    /**
     * @return int
     */
    public int getContentLength();

    /**
     * @return {@link Throwable}
     */
    public Throwable getException();

    /**
     * @return {@link InputStream}
     */
    public default InputStream getInputStream()
    {
        return Channels.newInputStream(getReadableByteChannel());
    }

    /**
     * @return {@link ReadableByteChannel}
     */
    public ReadableByteChannel getReadableByteChannel();

    /**
     * @return int
     */
    public int getStatus();
}
