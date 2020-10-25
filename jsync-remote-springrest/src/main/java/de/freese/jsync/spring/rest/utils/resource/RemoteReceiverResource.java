// Created: 09.10.2020
package de.freese.jsync.spring.rest.utils.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.springframework.core.io.WritableResource;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverResource extends AbstractJsyncResource implements WritableResource
{
    /**
     *
     */
    private final WritableByteChannel writableByteChannel;

    /**
     * Erstellt ein neues {@link RemoteReceiverResource} Object.
     *
     * @param fileName String
     * @param fileSize long
     * @param writableByteChannel {@link WritableByteChannel}
     */
    public RemoteReceiverResource(final String fileName, final long fileSize, final WritableByteChannel writableByteChannel)
    {
        super(fileName, fileSize);

        this.writableByteChannel = writableByteChannel;
    }

    /**
     * @see org.springframework.core.io.WritableResource#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return Channels.newOutputStream(writableChannel());
    }

    /**
     * @see org.springframework.core.io.WritableResource#isWritable()
     */
    @Override
    public boolean isWritable()
    {
        return true;
    }

    /**
     * @see de.freese.jsync.spring.rest.utils.resource.AbstractJsyncResource#readableChannel()
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.WritableResource#writableChannel()
     */
    @Override
    public WritableByteChannel writableChannel() throws IOException
    {
        return this.writableByteChannel;
    }
}
