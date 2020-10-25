// Created: 09.10.2020
package de.freese.jsync.spring.rest.utils.resource;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Thomas Freese
 */
public class RemoteSenderResource extends AbstractJsyncResource
{
    /**
     *
     */
    private final ReadableByteChannel readableByteChannel;

    /**
     * Erstellt ein neues {@link RemoteSenderResource} Object.
     *
     * @param fileName String
     * @param fileSize long
     * @param readableByteChannel {@link ReadableByteChannel}
     */
    public RemoteSenderResource(final String fileName, final long fileSize, final ReadableByteChannel readableByteChannel)
    {
        super(fileName, fileSize);

        this.readableByteChannel = readableByteChannel;
    }

    /**
     * @see de.freese.jsync.spring.rest.utils.resource.AbstractJsyncResource#isReadable()
     */
    @Override
    public boolean isReadable()
    {
        return true;
    }

    /**
     * @see de.freese.jsync.spring.rest.utils.resource.AbstractJsyncResource#readableChannel()
     */
    @Override
    public ReadableByteChannel readableChannel() throws IOException
    {
        return this.readableByteChannel;
    }
}
