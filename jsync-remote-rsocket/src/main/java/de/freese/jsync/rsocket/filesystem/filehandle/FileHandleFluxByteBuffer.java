// Created: 25.10.2020
package de.freese.jsync.rsocket.filesystem.filehandle;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import de.freese.jsync.filesystem.fileHandle.FileHandle;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class FileHandleFluxByteBuffer implements FileHandle
{
    /**
     *
     */
    private Flux<ByteBuffer> flux;

    /**
     * Erstellt ein neues {@link FileHandleFluxByteBuffer} Object.
     *
     * @param flux {@link Flux}
     */
    public FileHandleFluxByteBuffer(final Flux<ByteBuffer> flux)
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
