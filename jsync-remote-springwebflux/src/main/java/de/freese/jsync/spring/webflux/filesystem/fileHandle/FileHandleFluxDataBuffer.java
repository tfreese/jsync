// Created: 25.10.2020
package de.freese.jsync.spring.webflux.filesystem.fileHandle;

import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import org.springframework.core.io.buffer.DataBuffer;

import de.freese.jsync.filesystem.fileHandle.FileHandle;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class FileHandleFluxDataBuffer implements FileHandle
{
    /**
     *
     */
    private Flux<DataBuffer> flux;

    /**
     * Erstellt ein neues {@link FileHandleFluxDataBuffer} Object.
     *
     * @param flux {@link Flux}
     */
    public FileHandleFluxDataBuffer(final Flux<DataBuffer> flux)
    {
        super();

        this.flux = Objects.requireNonNull(flux, "flux required");
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#getHandle()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getHandle()
    {
        return (T) this.flux;
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#writeTo(java.nio.channels.WritableByteChannel, long)
     */
    @Override
    public void writeTo(final WritableByteChannel writableByteChannel, final long sizeOfFile) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}
