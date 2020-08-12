/**
 * Created: 13.11.2018
 */

package de.freese.jsync.utils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.SocketChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
public final class JSyncUtils
{
    /**
    *
    */
    private static final char[] HEX_CHARS =
    {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     *
     */
    private static final String[] SIZE_UNITS = new String[]
    {
            "B", "KB", "MB", "GB", "TB"
    };

    /**
     * String hex = javax.xml.bind.DatatypeConverter.printHexBinary(checksum);<br>
     * String hex = org.apache.commons.codec.binary.Hex.encodeHexString(messageDigest);<br>
     * String hex = String.format("%02x", element);<br>
     *
     * @param bytes byte[]
     * @return String
     */
    public static String bytesToHex(final byte[] bytes)
    {
        StringBuilder sbuf = new StringBuilder(bytes.length * 2);

        for (byte b : bytes)
        {
            // int temp = b & 0xFF;
            //
            // sbuf.append(HEX_CHARS[temp >> 4]);
            // sbuf.append(HEX_CHARS[temp & 0x0F]);

            sbuf.append(HEX_CHARS[(b & 0xF0) >>> 4]);
            sbuf.append(HEX_CHARS[b & 0x0F]);
        }

        return sbuf.toString().toUpperCase();
    }

    /**
     * SocketChannels werden NICHT geschlossen !
     *
     * @param closeable {@link Closeable}
     */
    public static void closeSilently(final Closeable closeable)
    {
        try
        {
            if ((closeable != null) && !(closeable instanceof SocketChannel))
            {
                closeable.close();
            }
        }
        catch (Exception ex)
        {
            // Ignore
        }
    }

    /**
     * Löscht das Verzeichnis rekursiv inklusive Dateien und Unterverzeichnisse.
     *
     * @param path {@link Path}
     * @throws IOException Falls was schief geht.
     */
    public static void deleteDirectoryRecursive(final Path path) throws IOException
    {
        if (path == null)
        {
            throw new NullPointerException("path required");
        }

        if (!Files.exists(path))
        {
            return;
        }

        if (!Files.isDirectory(path))
        {
            throw new IllegalArgumentException("path is not a dirctory: " + path);
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>()
        {
            /**
             * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
             */
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException
            {
                Files.delete(dir);

                return FileVisitResult.CONTINUE;
            }

            /**
             * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
             */
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException
            {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Mit 2 Nachkommastellen.
     *
     * @param value long
     * @param max long
     * @return float 0.0 - 100.0
     */
    public static float getPercent(final long value, final long max)
    {
        float progress = getProgress(value, max);
        float percentage = progress * 100F;

        return percentage;
    }

    /**
     * @param value long
     * @param max long
     * @return float 0.0 - 1.0
     */
    public static float getProgress(final long value, final long max)
    {
        if ((value <= 0) || (value > max))
        {
            // throw new IllegalArgumentException("invalid value: " + value);
            return 0.0F;
        }

        float progress = (float) value / (float) max;

        // // Nachkommastellen
        // int rounded = Math.round(progress * 100);
        // progress = rounded / 100F;

        return progress;
    }

    /**
     * @param hexString {@link CharSequence}
     * @return byte[]
     */
    public static byte[] hexToBytes(final CharSequence hexString)
    {
        if ((hexString.length() % 2) == 1)
        {
            throw new IllegalArgumentException("Invalid hexadecimal String supplied.");
        }

        byte[] bytes = new byte[hexString.length() / 2];

        for (int i = 0; i < hexString.length(); i += 2)
        {
            int firstDigit = Character.digit(hexString.charAt(i), 16);
            int secondDigit = Character.digit(hexString.charAt(i + 1), 16);

            if ((firstDigit < 0) || (secondDigit < 0))
            {
                throw new IllegalArgumentException("Invalid Hexadecimal Character in: " + hexString);
            }

            bytes[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
        }

        return bytes;
    }

    /**
     * Normalisiert den {@link URI#getPath()} je nach Betriebssystem.
     *
     * @param uri {@link URI}
     * @return String
     */
    public static String normalizedPath(final URI uri)
    {
        String path = uri.getPath();

        if (Options.IS_WINDOWS)
        {
            path = path.replace("\\", "/");

            if (path.startsWith("/"))
            {
                path = path.substring(1);
            }
        }
        else if (Options.IS_LINUX)
        {
            if (path.startsWith("//"))
            {
                path = path.substring(1);
            }
            else if (!path.startsWith("/"))
            {
                path = "/" + path;
            }
        }

        return path;
    }

    /**
     * Shutdown der {@link AsynchronousChannelGroup}.
     *
     * @param channelGroup {@link AsynchronousChannelGroup}
     * @param logger {@link Logger}
     */
    public static void shutdown(final AsynchronousChannelGroup channelGroup, final Logger logger)
    {
        logger.info("shutdown AsynchronousChannelGroup");

        if (channelGroup == null)
        {
            return;
        }

        channelGroup.shutdown();

        try
        {
            // Wait a while for existing tasks to terminate.
            if (!channelGroup.awaitTermination(10, TimeUnit.SECONDS))
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn("Timed out while waiting for channelGroup");
                }

                channelGroup.shutdownNow(); // Cancel currently executing tasks

                // Wait a while for tasks to respond to being cancelled
                if (!channelGroup.awaitTermination(5, TimeUnit.SECONDS))
                {
                    logger.error("ChannelGroup did not terminate");
                }
            }
        }
        catch (InterruptedException | IOException ex)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Interrupted while waiting for ChannelGroup");
            }

            // (Re-)Cancel if current thread also interrupted
            try
            {
                channelGroup.shutdownNow();
            }
            catch (IOException ex2)
            {
                logger.error("ChannelGroup did not terminate");
            }

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown des {@link ExecutorService}.
     *
     * @param executorService {@link ExecutorService}
     * @param logger {@link Logger}
     */
    public static void shutdown(final ExecutorService executorService, final Logger logger)
    {
        logger.info("shutdown ExecutorService");

        if (executorService == null)
        {
            logger.warn("ExecutorService is null");

            return;
        }

        executorService.shutdown();

        try
        {
            // Wait a while for existing tasks to terminate.
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
            {
                logger.warn("Timed out while waiting for ExecutorService");

                // Cancel currently executing tasks.
                for (Runnable remainingTask : executorService.shutdownNow())
                {
                    if (remainingTask instanceof Future)
                    {
                        ((Future<?>) remainingTask).cancel(true);
                    }
                }

                // Wait a while for tasks to respond to being cancelled.
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                {
                    logger.error("ExecutorService did not terminate");
                }
                else
                {
                    logger.info("ExecutorService terminated");
                }
            }
            else
            {
                logger.info("ExecutorService terminated");
            }
        }
        catch (InterruptedException iex)
        {
            logger.warn("Interrupted while waiting for ExecutorService");

            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param timeUnit {@link TimeUnit}
     * @param timeout long
     */
    public static void sleep(final TimeUnit timeUnit, final long timeout)
    {
        try
        {
            timeUnit.sleep(timeout);
        }
        catch (InterruptedException ex)
        {
            // Ignoren
        }
    }

    /**
     * @param size long
     * @return String, z.B. '___,___ MB'
     */
    public static String toHumanReadableSize(final long size)
    {
        int unitIndex = 0;

        if (size > 0)
        {
            unitIndex = (int) (Math.log10(size) / 3);
        }

        double unitValue = 1 << (unitIndex * 10);

        // String readableSize = new DecimalFormat("#,##0.#").format(size / unitValue) + " " + SIZE_UNITS[unitIndex];
        // String readableSize = String.format("%7.0f %s", size / unitValue, SIZE_UNITS[unitIndex]);
        String readableSize = String.format("%.0f %s", size / unitValue, SIZE_UNITS[unitIndex]);

        return readableSize;
    }

    /**
     * Erstellt ein neues {@link JSyncUtils} Object.
     */
    private JSyncUtils()
    {
        super();
    }
}
