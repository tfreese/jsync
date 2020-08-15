/**
 * Created: 17.11.2018
 */

package de.freese.jsync.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.LongConsumer;

/**
 * @author Thomas Freese
 */
public final class DigestUtils
{
    /**
     *
     */
    public static final int BUFFER_SIZE = 8 * 1024;

    /**
     * Erzeugt den {@link MessageDigest} für die Generierung der Prüfsumme.<br>
     * <br>
     * Every implementation of the Java platform is required to support the following standard MessageDigest algorithms:<br>
     * MD5<br>
     * SHA-1<br>
     * SHA-256<br>
     *
     * @param algorithm String
     * @return {@link MessageDigest}
     * @throws RuntimeException Falls was schief geht.
     */
    public static MessageDigest createMessageDigest(final String algorithm)
    {
        try
        {
            return MessageDigest.getInstance(algorithm);
        }
        catch (final NoSuchAlgorithmException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return {@link MessageDigest}
     */
    public static MessageDigest createSha256Digest()
    {
        return createMessageDigest("SHA-256");
    }

    /**
     * Die Position des {@link ByteBuffer} wird wieder auf den Ursprungs-Wert gesetzt.<br>
     * {@link ByteBuffer#position()}<br>
     * {@link MessageDigest#update(ByteBuffer)}<br>
     * {@link ByteBuffer#position(int)}<br>
     *
     * @param messageDigest {@link MessageDigest}
     * @param byteBuffer {@link ByteBuffer}
     */
    public static void digest(final MessageDigest messageDigest, final ByteBuffer byteBuffer)
    {
        int position = byteBuffer.position();

        messageDigest.update(byteBuffer);

        byteBuffer.position(position);
    }

    /**
     * @param messageDigest {@link MessageDigest}
     * @return String
     */
    public static String digestAsHex(final MessageDigest messageDigest)
    {
        final byte[] digest = messageDigest.digest();
        final String hex = JSyncUtils.bytesToHex(digest);

        return hex;
    }

    /**
     * @param bytes byte[]
     * @return byte[]
     */
    public static byte[] sha256Digest(final byte[] bytes)
    {
        final MessageDigest messageDigest = createSha256Digest();

        return messageDigest.digest(bytes);
    }

    /**
     * Der {@link InputStream} wird NICHT geschlossen !
     *
     * @param inputStream {@link InputStream}
     * @return byte[]
     * @throws IOException Falls was schief geht.
     */
    public static byte[] sha256Digest(final InputStream inputStream) throws IOException
    {
        final MessageDigest messageDigest = createSha256Digest();
        final byte[] buffer = new byte[BUFFER_SIZE];

        int bytesRead = -1;

        while ((bytesRead = inputStream.read(buffer)) != -1)
        {
            messageDigest.update(buffer, 0, bytesRead);
        }

        return messageDigest.digest();
    }

    /**
     * @param path {@link Path}
     * @param bufferSize int
     * @param consumerBytesRead {@link LongConsumer}; optional
     * @return byte[]
     * @throws IOException Falls was schief geht.
     */
    public static byte[] sha256Digest(final Path path, final int bufferSize, final LongConsumer consumerBytesRead) throws IOException
    {
        final MessageDigest messageDigest = createSha256Digest();
        byte[] bytes = null;

        LongConsumer consumer = consumerBytesRead != null ? consumerBytesRead : i -> {
            // Empty
        };

        consumer.accept(0);

        try (ReadableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ))
        {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            long bytesReadSum = 0;
            // long bytesRead = 0;

            while (channel.read(buffer) != -1)
            {
                bytesReadSum += buffer.position();
                // bytesRead = buffer.position();

                consumer.accept(bytesReadSum);

                messageDigest.update(buffer);
                buffer.clear();
            }

            bytes = messageDigest.digest();
        }

        return bytes;
    }

    /**
     * @param path {@link Path}
     * @param bufferSize int
     * @param consumerBytesRead {@link LongConsumer}; optional
     * @return String
     */
    public static String sha256DigestAsHex(final Path path, final int bufferSize, final LongConsumer consumerBytesRead)
    {
        try
        {
            final byte[] bytes = sha256Digest(path, bufferSize, consumerBytesRead);

            final String hex = JSyncUtils.bytesToHex(bytes);

            return hex;
        }
        catch (IOException iex)
        {
            throw new UncheckedIOException(iex);
        }
    }

    /**
     * Erstellt ein neues {@link DigestUtils} Object.
     */
    private DigestUtils()
    {
        super();
    }
}
