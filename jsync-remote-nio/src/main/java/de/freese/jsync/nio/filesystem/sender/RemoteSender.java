// Created: 29.09.2020
package de.freese.jsync.nio.filesystem.sender;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.remote.api.JsyncConnectionFactory;

/**
 * @author Thomas Freese
 */
public class RemoteSender extends AbstractSender
{
    /**
    *
    */
    private final JsyncConnectionFactory connectionFactory;

    /**
     * Erstellt ein neues {@link RemoteSender} Object.
     *
     * @param connectionFactory {@link JsyncConnectionFactory}
     */
    public RemoteSender(final JsyncConnectionFactory connectionFactory)
    {
        super();

        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory required");
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.connectionFactory.connect(uri);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        // TODO Auto-generated method stub

    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String, long)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer buffer)
    {
        // TODO Auto-generated method stub

    }
}
