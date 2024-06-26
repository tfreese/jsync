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
    @Override
    public void createDirectory(final String baseDir, final String relativePath) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().writeString(buffer, relativePath);
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

    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.TARGET_DELETE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().writeString(buffer, relativePath);
                getSerializer().writeBoolean(buffer, followSymLinks);
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

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.TARGET_CHECKSUM);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return generateSyncItems(baseDir, followSymLinks, PathFilterNoOp.INSTANCE, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
    }

    @Override
    public void update(final String baseDir, final SyncItem syncItem) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.TARGET_UPDATE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().write(buffer, syncItem);
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

    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum, final LongConsumer consumerChecksumBytesRead) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.TARGET_VALIDATE_FILE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().write(buffer, syncItem);
                getSerializer().writeBoolean(buffer, withChecksum);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).map(buffer -> {
                final long value = getSerializer().readLong(buffer);
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

    @Override
    public Flux<Long> writeFile(final String baseDir, final String relativeFile, final long sizeOfFile, final Flux<ByteBuffer> fileFlux) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.TARGET_WRITE_FILE));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().writeString(buffer, relativeFile);
                getSerializer().writeLong(buffer, sizeOfFile);
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
                final long bytesWritten = getSerializer().readLong(buffer);
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
