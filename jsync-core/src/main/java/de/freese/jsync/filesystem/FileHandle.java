// Created: 24.10.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import org.springframework.core.io.buffer.DataBuffer;
import de.freese.jsync.filesystem.sender.Sender;
import reactor.core.publisher.Flux;

/**
 * Enthält die Daten einer Datei.<br>
 * Nur eine der Daten-Strukturen ist durch den {@link Sender} gesetzt.<br>
 * Der Receiver nimmt sich dann die für ihn passende Struktur.
 *
 * @author Thomas Freese
 */
public class FileHandle
{
    /**
    *
    */
    private Flux<ByteBuffer> fluxByteBuffer;

    /**
     *
     */
    private Flux<DataBuffer> fluxDataBuffer;

    /**
     *
     */
    private ReadableByteChannel readableByteChannel;

    /**
     * Erstellt ein neues {@link FileHandle} Object.
     */
    public FileHandle()
    {
        super();
    }

    /**
     * @param fluxByteBuffer Flux<ByteBuffer>
     * @return {@link FileHandle}
     */
    public FileHandle fluxByteBuffer(final Flux<ByteBuffer> fluxByteBuffer)
    {
        setFluxByteBuffer(fluxByteBuffer);

        return this;
    }

    /**
     * @param fluxDataBuffer Flux<DataBuffer>
     * @return {@link FileHandle}
     */
    public FileHandle fluxDataBuffer(final Flux<DataBuffer> fluxDataBuffer)
    {
        setFluxDataBuffer(fluxDataBuffer);

        return this;
    }

    /**
     * @return {@link Flux}<ByteBuffer>
     */
    public Flux<ByteBuffer> getFluxByteBuffer()
    {
        return this.fluxByteBuffer;
    }

    /**
     * @return {@link Flux}<DataBuffer>
     */
    public Flux<DataBuffer> getFluxDataBuffer()
    {
        return this.fluxDataBuffer;
    }

    /**
     * @return {@link ReadableByteChannel}
     */
    public ReadableByteChannel getReadableByteChannel()
    {
        return this.readableByteChannel;
    }

    /**
     * @param readableByteChannel {@link ReadableByteChannel}
     * @return {@link FileHandle}
     */
    public FileHandle readableByteChannel(final ReadableByteChannel readableByteChannel)
    {
        setReadableByteChannel(readableByteChannel);

        return this;
    }

    /**
     * @param fluxByteBuffer {@link Flux}<ByteBuffer>
     */
    public void setFluxByteBuffer(final Flux<ByteBuffer> fluxByteBuffer)
    {
        this.fluxByteBuffer = Objects.requireNonNull(fluxByteBuffer, "fluxByteBuffer required");
    }

    /**
     * @param fluxDataBuffer {@link Flux}<DataBuffer>
     */
    public void setFluxDataBuffer(final Flux<DataBuffer> fluxDataBuffer)
    {
        this.fluxDataBuffer = Objects.requireNonNull(fluxDataBuffer, "fluxDataBuffer required");
    }

    /**
     * @param readableByteChannel {@link ReadableByteChannel}
     */
    public void setReadableByteChannel(final ReadableByteChannel readableByteChannel)
    {
        this.readableByteChannel = Objects.requireNonNull(readableByteChannel, "readableByteChannel required");
    }
}
