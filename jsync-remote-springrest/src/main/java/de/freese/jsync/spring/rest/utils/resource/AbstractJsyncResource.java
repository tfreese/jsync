// Created: 10.10.2020
package de.freese.jsync.spring.rest.utils.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.springframework.core.io.Resource;

/**
 * @author Thomas Freese
 */
public abstract class AbstractJsyncResource implements Resource
{
    /**
     *
     */
    private final String fileName;

    /**
     *
     */
    private final long fileSize;

    /**
     * Erstellt ein neues {@link AbstractJsyncResource} Object.
     *
     * @param fileName String
     * @param fileSize long
     */
    public AbstractJsyncResource(final String fileName, final long fileSize)
    {
        super();

        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    /**
     * @see org.springframework.core.io.Resource#contentLength()
     */
    @Override
    public long contentLength() throws IOException
    {
        return this.fileSize;
    }

    /**
     * @see org.springframework.core.io.Resource#createRelative(java.lang.String)
     */
    @Override
    public Resource createRelative(final String relativePath) throws IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.Resource#exists()
     */
    @Override
    public boolean exists()
    {
        return true;
    }

    /**
     * @see org.springframework.core.io.Resource#getDescription()
     */
    @Override
    public String getDescription()
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.Resource#getFile()
     */
    @Override
    public File getFile() throws IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.Resource#getFilename()
     */
    @Override
    public String getFilename()
    {
        return this.fileName;
    }

    /**
     * @see org.springframework.core.io.InputStreamSource#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException
    {
        return Channels.newInputStream(readableChannel());
    }

    /**
     * @see org.springframework.core.io.Resource#getURI()
     */
    @Override
    public URI getURI() throws IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.Resource#getURL()
     */
    @Override
    public URL getURL() throws IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see org.springframework.core.io.Resource#isFile()
     */
    @Override
    public boolean isFile()
    {
        return false;
    }

    /**
     * @see org.springframework.core.io.Resource#isOpen()
     */
    @Override
    public boolean isOpen()
    {
        return false;
    }

    /**
     * @see org.springframework.core.io.Resource#isReadable()
     */
    @Override
    public boolean isReadable()
    {
        return false;
    }

    /**
     * @see org.springframework.core.io.Resource#lastModified()
     */
    @Override
    public long lastModified() throws IOException
    {
        return 0;
    }

    /**
     * @see org.springframework.core.io.Resource#readableChannel()
     */
    @Override
    public abstract ReadableByteChannel readableChannel() throws IOException;

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("fileName=").append(this.fileName);
        builder.append(", fileSize=").append(this.fileSize);
        builder.append("]");

        return builder.toString();
    }
}
