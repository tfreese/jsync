// Created: 13.11.2018

package de.freese.jsync.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.utils.io.FileVisitorDelete;

/**
 * @author Thomas Freese
 */
public final class JSyncUtils
{
    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    private static final FileVisitOption[] FILEVISITOPTION_NO_SYNLINKS = {};

    /**
     * @see Files#walk(Path, FileVisitOption...)
     */
    private static final FileVisitOption[] FILEVISITOPTION_WITH_SYMLINKS =
    {
            FileVisitOption.FOLLOW_LINKS
    };

    /**
     *
     */
    private static final char[] HEX_CHARS =
    {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * @see Files#exists(Path, LinkOption...)
     * @see Files#notExists(Path, LinkOption...)
     * @see Files#isDirectory(Path, LinkOption...)
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    private static final LinkOption[] LINKOPTION_NO_SYMLINKS =
    {
            LinkOption.NOFOLLOW_LINKS
    };

    /**
     * @see Files#exists(Path, LinkOption...)
     * @see Files#notExists(Path, LinkOption...)
     * @see Files#isDirectory(Path, LinkOption...)
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     * @see Files#readAttributes(Path, String, LinkOption...)
     */
    private static final LinkOption[] LINKOPTION_WITH_SYMLINKS = {};

    /**
    *
    */
    private static final Pattern PATTERN_FILTER = Pattern.compile("[;,]");

    /**
     *
     */
    private static final String[] SIZE_UNITS =
    {
            "B", "KB", "MB", "GB", "TB", "PB", "EB"
    };

    /**
     * String hex = javax.xml.bind.DatatypeConverter.printHexBinary(checksum);<br>
     * String hex = org.apache.commons.codec.binary.Hex.encodeHexString(messageDigest);<br>
     * String hex = String.format("%02x", element);<br>
     *
     * @param bytes byte[]
     *
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
     * @param channel {@link Channel}
     */
    public static void close(final Channel channel)
    {
        if ((channel != null) && channel.isOpen())
        {
            try
            {
                if (channel instanceof FileChannel)
                {
                    ((FileChannel) channel).force(false);
                }

                channel.close();
            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }
        }
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
     * Ein Verzeichnis wird rekursiv inklusive aller Dateien und Unterverzeichnisse gelöscht.
     *
     * @param path {@link Path}
     * @param followSymLinks boolean
     *
     * @throws IOException Falls was schief geht.
     */
    public static void delete(final Path path, final boolean followSymLinks) throws IOException
    {
        if (path == null)
        {
            throw new IllegalArgumentException("path required");
        }

        LinkOption[] linkOptions = getLinkOptions(followSymLinks);

        if (!Files.exists(path, linkOptions))
        {
            return;
        }

        FileVisitOption[] fileVisitOptions = getFileVisitOptions(followSymLinks);

        if (Files.isDirectory(path, linkOptions))
        {
            Files.walkFileTree(path, Set.of(fileVisitOptions), Integer.MAX_VALUE, new FileVisitorDelete());
        }
        else
        {
            Files.delete(path);
        }
    }

    /**
     * @param followSymLinks boolean
     *
     * @return {@link FileVisitOption}
     */
    public static FileVisitOption[] getFileVisitOptions(final boolean followSymLinks)
    {
        return followSymLinks ? FILEVISITOPTION_WITH_SYMLINKS : FILEVISITOPTION_NO_SYNLINKS;
    }

    /**
     * @param followSymLinks boolean
     *
     * @return {@link LinkOption}
     */
    public static LinkOption[] getLinkOptions(final boolean followSymLinks)
    {
        return followSymLinks ? LINKOPTION_WITH_SYMLINKS : LINKOPTION_NO_SYMLINKS;
    }

    /**
     * @return String
     */
    public static String getOsName()
    {
        return System.getProperty("os.name");
    }

    /**
     * Mit 2 Nachkommastellen.
     *
     * @param value long
     * @param max long
     *
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
     *
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
     *
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
     * @return boolean
     */
    public static boolean isLinux()
    {
        String os = getOsName().toLowerCase();

        return os.contains("linux");
    }

    /**
     * @return boolean
     */
    public static boolean isUnix()
    {
        String os = getOsName().toLowerCase();

        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    /**
     * @return boolean
     */
    public static boolean isWindows()
    {
        String os = getOsName().toLowerCase();

        return os.startsWith("win");
    }

    /**
     * Entfernt führende '//' und das abschließende '/'.
     *
     * @param uri {@link URI}
     *
     * @return String
     */
    public static String normalizePath(final URI uri)
    {
        // String path = Paths.get(uri).toString();
        String path = uri.getPath();

        if (path.startsWith("//"))
        {
            path = path.substring(1);
        }

        if (isWindows() && path.startsWith("/"))
        {
            path = path.substring(1);
        }

        if (path.endsWith("/"))
        {
            path = path.substring(0, path.length() - 1);
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
            // Empty
        }
    }

    /**
     * Aufsplitten nach ';' oder ',' um ein Set für den {@link PathFilter} zu erzeugen.
     *
     * @param value String
     *
     * @return {@link Set}
     */
    public static Set<String> toFilter(final String value)
    {
        if ((value == null) || value.isBlank())
        {
            return Collections.emptySet();
        }

        String[] splits = PATTERN_FILTER.split(value);

        return Arrays.stream(splits).map(String::trim).collect(Collectors.toSet());
    }

    /**
     * @param size long
     *
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
        String readableSize = String.format("%.1f %s", size / unitValue, SIZE_UNITS[unitIndex]);

        return readableSize;

        // Variante 2: Micrometer
        // io.micrometer.core.instrument.logging.LoggingMeterRegistry.Printer.humanReadableByteCount(double)
        // int unit = 1024;
        // if (bytes < unit || Double.isNaN(bytes)) return decimalOrNan(bytes) + " B";
        // int exp = (int) (Math.log(bytes) / Math.log(unit));
        // String pre = "KMGTPE".charAt(exp - 1) + "i";
        // return decimalOrNan(bytes / Math.pow(unit, exp)) + " " + pre + "B";

        // Variante 3:
        // double divider = 1D;
        // String unit = "";
        //
        // if (size < 1024)
        // {
        // divider = 1D;
        // unit = "B";
        // }
        // else if (size < 1_048_576)
        // {
        // divider = 1024D;
        // unit = "KB";
        // }
        // else if (size < 1_073_741_824)
        // {
        // divider = 1_048_576D;
        // unit = "MB";
        // }
        // else if (size < (1_099_511_627_776))
        // {
        // divider = 1_073_741_824D;
        // unit = "GB";
        // }
        //
        // String readableSize = String.format("%.1f %s", size / divider, unit);
        //
        // return readableSize;

        // Variante 4:
        // double value = Math.abs(size);
        //
        // if (value < 1024D)
        // {
        // return size + " B";
        // }
        //
        // CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        //
        // while (value > 1024D)
        // {
        // value /= 1024;
        // ci.next();
        // }
        //
        // return String.format("%.1f %cB", value, ci.previous());
    }

    /**
     * Erstellt ein neues {@link JSyncUtils} Object.
     */
    private JSyncUtils()
    {
        super();
    }
}
