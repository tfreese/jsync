// Created: 17.11.2018
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

import de.freese.jsync.Options;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public final class DigestUtils
{
    /**
     * The {@link ByteBuffer} Position is reset to old Position.<br>
     * {@link ByteBuffer#position()}<br>
     * {@link MessageDigest#update(ByteBuffer)}<br>
     * {@link ByteBuffer#position(int)}<br>
     */
    public static void digest(final MessageDigest messageDigest, final ByteBuffer byteBuffer)
    {
        int position = byteBuffer.position();

        messageDigest.update(byteBuffer);

        byteBuffer.position(position);
    }

    public static String digestAsHex(final MessageDigest messageDigest)
    {
        final byte[] digest = messageDigest.digest();

        return JSyncUtils.bytesToHex(digest);
    }

    public static byte[] sha256Digest(final byte[] bytes)
    {
        final MessageDigest messageDigest = createSha256Digest();

        return messageDigest.digest(bytes);
    }

    /**
     * The {@link InputStream} will <strong>NOT</strong> be closed !
     */
    public static byte[] sha256Digest(final InputStream inputStream) throws IOException
    {
        final MessageDigest messageDigest = createSha256Digest();
        final byte[] buffer = new byte[Options.BUFFER_SIZE];

        int bytesRead = -1;

        while ((bytesRead = inputStream.read(buffer)) != -1)
        {
            messageDigest.update(buffer, 0, bytesRead);
        }

        return messageDigest.digest();

        // return sha256Digest(Channels.newChannel(inputStream), Options.DATABUFFER_SIZE, i -> {
        // });
    }

    public static String sha256DigestAsHex(final Path path)
    {
        return sha256DigestAsHex(path, i ->
        {
            // Empty
        });
    }

    public static String sha256DigestAsHex(final Path path, final LongConsumer consumerBytesRead)
    {
        try (ReadableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ))
        {
            byte[] bytes = sha256Digest(channel, consumerBytesRead);

            return JSyncUtils.bytesToHex(bytes);
        }
        catch (IOException iex)
        {
            throw new UncheckedIOException(iex);
        }
    }

    /**
     * Creates the {@link MessageDigest} for Checksum generation.<br>
     * <br>
     * Every implementation of the Java platform is required to support the following standard MessageDigest algorithms:<br>
     * MD5<br>
     * SHA-1<br>
     * SHA-256<br>
     */
    private static MessageDigest createMessageDigest(final String algorithm)
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

    private static MessageDigest createSha256Digest()
    {
        return createMessageDigest("SHA-256");
    }

    /**
     * @param consumerBytesRead {@link LongConsumer}; optional
     */
    private static byte[] sha256Digest(final ReadableByteChannel readableByteChannel, final LongConsumer consumerBytesRead) throws IOException
    {
        MessageDigest messageDigest = createSha256Digest();
        byte[] bytes = null;

        if (consumerBytesRead != null)
        {
            consumerBytesRead.accept(0);
        }

        ByteBuffer buffer = ByteBufferPool.DEFAULT.get();

        try
        {
            long bytesRead = 0;

            while (readableByteChannel.read(buffer) != -1)
            {
                bytesRead += buffer.position();

                if (consumerBytesRead != null)
                {
                    consumerBytesRead.accept(bytesRead);
                }

                buffer.flip();
                messageDigest.update(buffer);
                buffer.clear();
            }

            bytes = messageDigest.digest();
        }
        finally
        {
            ByteBufferPool.DEFAULT.free(buffer);
        }

        return bytes;
    }

    private DigestUtils()
    {
        super();
    }
}
