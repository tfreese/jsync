// Created: 25.10.2020
package de.freese.jsync.filesystem.fileHandle;

import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import de.freese.jsync.utils.JSyncUtils;

/**
 * Lokales Kopieren von Dateien.
 *
 * @author Thomas Freese
 */
public class FileHandleFileChannel implements FileHandle
{
    /**
     *
     */
    private final FileChannel fileChannel;

    /**
     * Erstellt ein neues {@link FileHandleFileChannel} Object.
     *
     * @param fileChannel {@link FileChannel}
     */
    public FileHandleFileChannel(final FileChannel fileChannel)
    {
        super();

        this.fileChannel = Objects.requireNonNull(fileChannel, "fileChannel required");
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#close()
     */
    @Override
    public void close() throws Exception
    {
        this.fileChannel.close();
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#getHandle()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getHandle()
    {
        return (T) this.fileChannel;
    }

    /**
     * @see de.freese.jsync.filesystem.fileHandle.FileHandle#writeTo(java.nio.channels.WritableByteChannel, long)
     */
    @Override
    public void writeTo(final WritableByteChannel writableByteChannel, final long sizeOfFile) throws Exception
    {
        // original - apparently has trouble copying large files on Windows
        // fileChannel.transferTo(0, fileChannel.size(), writableByteChannel);

        if (JSyncUtils.isLinux() || JSyncUtils.isUnix())
        {
            this.fileChannel.transferTo(0, this.fileChannel.size(), writableByteChannel);
        }
        else if (JSyncUtils.isWindows())
        {
            // Der Einzeilige Transfer wie bei Linux verursacht bei Windows Fehler bei zu großen Dateien.
            // Max. Blocksize für Windows: (64Mb - 32Kb)
            long maxWindowsBlockSize = (1024 * 1024 * 64L) - (1024 * 32L);

            long count = sizeOfFile;

            if (count > maxWindowsBlockSize)
            {
                count = maxWindowsBlockSize;
            }

            long totalTransfered = 0;

            while (totalTransfered < sizeOfFile)
            {
                if ((sizeOfFile - totalTransfered) < count)
                {
                    count = sizeOfFile - totalTransfered;
                }

                long transfered = this.fileChannel.transferTo(totalTransfered, count, writableByteChannel);

                totalTransfered += transfered;
            }
        }
    }
}
