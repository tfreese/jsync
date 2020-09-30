// Created: 29.09.2020
package de.freese.jsync.remote.api;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import de.freese.jsync.model.JSyncCommand;

/**
 * @author Thomas Freese
 */
public interface JsyncRequest
{
    /**
     * @return {@link JsyncResponse}
     */
    public JsyncResponse execute();

    /**
     * @return {@link OutputStream}
     */
    public default OutputStream getOutputStream()
    {
        return Channels.newOutputStream(getWritableByteChannel());
    }

    /**
     * @return {@link WritableByteChannel}
     */
    public WritableByteChannel getWritableByteChannel();

    /**
     * @param value Object
     * @param type Class
     * @return {@link JsyncRequest}
     */
    public JsyncRequest param(final Object value, final Class<?> type);

    /**
     * @param command {@link JSyncCommand}
     */
    public void setCommand(JSyncCommand command);
}
