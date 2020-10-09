// Created: 09.10.2020
package de.freese.jsync.nio.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.springframework.core.io.Resource;

/**
 * @author Thomas Freese
 */
public class RemoteSenderResource implements Resource
{
    /**
     * Erstellt ein neues {@link RemoteSenderResource} Object.
     */
    public RemoteSenderResource()
    {
        super();
    }

    /**
     * @see org.springframework.core.io.Resource#contentLength()
     */
    @Override
    public long contentLength() throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see org.springframework.core.io.Resource#createRelative(java.lang.String)
     */
    @Override
    public Resource createRelative(final String relativePath) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#exists()
     */
    @Override
    public boolean exists()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see org.springframework.core.io.Resource#getDescription()
     */
    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#getFile()
     */
    @Override
    public File getFile() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#getFilename()
     */
    @Override
    public String getFilename()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.InputStreamSource#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#getURI()
     */
    @Override
    public URI getURI() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#getURL()
     */
    @Override
    public URL getURL() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.springframework.core.io.Resource#lastModified()
     */
    @Override
    public long lastModified() throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
