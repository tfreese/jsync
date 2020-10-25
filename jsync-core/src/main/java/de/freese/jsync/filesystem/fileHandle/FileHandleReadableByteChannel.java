// Created: 25.10.2020
package de.freese.jsync.filesystem.fileHandle;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class FileHandleReadableByteChannel implements FileHandle
{
    /**
     *
     */
    private final ReadableByteChannel readableByteChannel;

    /**
     * Erstellt ein neues {@link FileHandleReadableByteChannel} Object.
     *
     * @param readableByteChannel {@link ReadableByteChannel}
     */
    public FileHandleReadableByteChannel(final ReadableByteChannel readableByteChannel)
    {
        super();

        this.readableByteChannel = Objects.requireNonNull(readableByteChannel, "readableByteChannel required");
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#close()
     */
    @Override
    public void close() throws Exception
    {
        this.readableByteChannel.close();
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#getHandle()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getHandle()
    {
        return (T) this.readableByteChannel;
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#writeTo(java.nio.channels.WritableByteChannel, long)
     */
    @Override
    public void writeTo(final WritableByteChannel writableByteChannel, final long sizeOfFile) throws Exception
    {
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().allocate();

        try
        {
            long totalWritten = 0;

            while (totalWritten < sizeOfFile)
            {
                byteBuffer.clear();
                this.readableByteChannel.read(byteBuffer);
                byteBuffer.flip();

                while (byteBuffer.hasRemaining())
                {
                    int bytesWritten = writableByteChannel.write(byteBuffer);

                    totalWritten += bytesWritten;
                }
            }
        }
        finally
        {
            ByteBufferPool.getInstance().release(byteBuffer);
        }
    }
}
