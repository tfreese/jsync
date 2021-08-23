// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class RemoteSenderNio extends AbstractNioFileSystem implements Sender
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.SOURCE_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, de.freese.jsync.filter.PathFilter)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter)
    {
        return generateSyncItems(baseDir, followSymLinks, pathFilter, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        SocketChannel channel = getChannelPool().obtain();

        try
        {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, JSyncCommand.SOURCE_READ_FILE);
            });

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativeFile);
                getSerializer().writeTo(buffer, sizeOfFile);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response lesen
            return getFrameProtocol().readAll(channel);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            getChannelPool().free(channel);
        }
    }
}
