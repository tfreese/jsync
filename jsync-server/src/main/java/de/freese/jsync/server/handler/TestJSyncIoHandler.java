/**
 * Created: 04.11.2018
 */

package de.freese.jsync.server.handler;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.io.MonitoringReadableByteChannel;

/**
 * Verarbeitet den Request und Response.<br>
 * Test-JSync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see IoHandler
 */
public class TestJSyncIoHandler extends AbstractIoHandler
{
    /**
     * Erstellt ein neues {@link TestJSyncIoHandler} Object.
     */
    public TestJSyncIoHandler()
    {
        super();
    }

    /**
     * @see de.freese.jsync.server.handler.IoHandler#read(java.nio.channels.SelectionKey, org.slf4j.Logger)
     */
    @SuppressWarnings("resource")
    @Override
    public void read(final SelectionKey selectionKey, final Logger logger) throws Exception
    {
        ReadableByteChannel inChannel = (ReadableByteChannel) selectionKey.channel();

        // Header lesen.
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);

        long bytesTransferred = inChannel.read(buffer);
        buffer.flip();

        long fileSize = buffer.getLong();
        byte[] fileNameBytes = new byte[buffer.getInt()];
        buffer.get(fileNameBytes);

        String fileName = new String(fileNameBytes, getCharset());

        Path pathDst = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        // Files.deleteIfExists(pathDst); // Wird schon durch die StandardOpenOption#TRUNCATE_EXISTING gemacht.

        BiConsumer<Long, Long> monitor =
                (written, gesamt) -> System.out.printf("Write: %d / %d = %.2f %%%n", written, gesamt, JSyncUtils.getPercent(written, gesamt));
        inChannel = new MonitoringReadableByteChannel(inChannel, monitor, fileSize);

        try (FileChannel outChannel = FileChannel.open(pathDst, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
        {
            outChannel.write(buffer);
            buffer.clear();

            while (inChannel.read(buffer) > 0)
            {
                bytesTransferred += buffer.position();

                buffer.flip();

                while (buffer.hasRemaining())
                {
                    logger.debug("Transfered data for file: {} / {}", bytesTransferred, pathDst);
                    outChannel.write(buffer);
                }

                buffer.clear();
            }

            outChannel.force(true);
        }
    }

    /**
     * @see de.freese.jsync.server.handler.IoHandler#write(java.nio.channels.SelectionKey, org.slf4j.Logger)
     */
    @Override
    public void write(final SelectionKey selectionKey, final Logger logger) throws Exception
    {
        // NOOP
    }
}
