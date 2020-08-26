// Created: 05.04.2018
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractFileSystem implements FileSystem
{
    /**
     *
     */
    private final List<ByteBuffer> bufferPool = new ArrayList<>();

    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
    *
    */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erzeugt eine neue Instanz von {@link AbstractFileSystem}.
     */
    public AbstractFileSystem()
    {
        super();

        // this.baseUri = Objects.requireNonNull(baseUri, "baseUri required");
        // this.basePath = Paths.get(JSyncUtils.normalizedPath(baseUri));
    }

    /**
     * @return {@link ByteBuffer}
     */
    protected ByteBuffer getBuffer()
    {
        getLock().lock();

        try
        {
            ByteBuffer buffer = null;

            if (this.bufferPool.isEmpty())
            {
                buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
            }
            else
            {
                buffer = this.bufferPool.remove(0);
            }

            return buffer;
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return {@link ReentrantLock}
     */
    protected ReentrantLock getLock()
    {
        return this.lock;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    protected void releaseBuffer(final ByteBuffer buffer)
    {
        getLock().lock();

        try
        {
            this.bufferPool.add(buffer);
        }
        finally
        {
            getLock().unlock();
        }
    }
}
