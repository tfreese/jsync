// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverNio extends AbstractNioFileSystem implements Receiver {
    /**
     * @see de.freese.jsync.filesystem.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath) {
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativePath);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks) {
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.TARGET_DELETE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativePath);
                getSerializer().writeTo(buffer, followSymLinks);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.TARGET_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, de.freese.jsync.filter.PathFilter)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return generateSyncItems(baseDir, followSymLinks, PathFilterNoOp.INSTANCE, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem) {
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.TARGET_UPDATE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, syncItem);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean, java.util.function.LongConsumer)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer consumerChecksumBytesRead) {
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.TARGET_VALIDATE_FILE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, syncItem);
                getSerializer().writeTo(buffer, withChecksum);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).map(buffer -> {
                long value = getSerializer().readFrom(buffer, Long.class);
                getFrameProtocol().getBufferPool().free(buffer);

                return value;
            }).doOnNext(consumerChecksumBytesRead::accept).subscribe();
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.Receiver#writeFile(java.lang.String, java.lang.String, long, reactor.core.publisher.Flux)
     */
    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux) {
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.TARGET_WRITE_FILE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativeFile);
                getSerializer().writeTo(buffer, sizeOfFile);
            });

            fileFlux.subscribe(buffer -> {
                try {
                    getFrameProtocol().writeData(channel, buffer);
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            return getFrameProtocol().readAll(channel).map(buffer -> {
                long bytesWritten = getSerializer().readFrom(buffer, Long.class);
                getFrameProtocol().getBufferPool().free(buffer);

                return bytesWritten;
            });
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }
}
