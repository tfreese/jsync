// Created: 17.09.2020
package de.freese.jsync.spring.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;

/**
 * @author Thomas Freese
 */
public class ByteBufferHttpMessageConverter extends AbstractHttpMessageConverter<ByteBuffer>
{
    /**
    *
    */
    private final int bufferSize;

    /**
     *
     */
    private final Supplier<ByteBuffer> byteBufferSupplier;

    /**
     * Erstellt ein neues {@link ByteBufferHttpMessageConverter} Object.
     *
     * @param bufferSize int
     * @param byteBufferSupplier {@link Supplier}
     */
    public ByteBufferHttpMessageConverter(final int bufferSize, final Supplier<ByteBuffer> byteBufferSupplier)
    {
        super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);

        if (bufferSize <= 0)
        {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }

        this.bufferSize = bufferSize;
        this.byteBufferSupplier = Objects.requireNonNull(byteBufferSupplier, "byteBufferSupplier required");
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#getContentLength(java.lang.Object, org.springframework.http.MediaType)
     */
    @Override
    protected Long getContentLength(final ByteBuffer t, final MediaType contentType) throws IOException
    {
        return (long) t.remaining();
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
     */
    @Override
    protected ByteBuffer readInternal(final Class<? extends ByteBuffer> clazz, final HttpInputMessage inputMessage) throws IOException
    {
        long contentLength = inputMessage.getHeaders().getContentLength();
        // ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength);

        ByteBuffer byteBuffer = this.byteBufferSupplier.get();
        byteBuffer.clear();

        InputStream inputStream = inputMessage.getBody();
        byte[] buffer = new byte[this.bufferSize];
        int bytesRead = 0;

        // while ((bytesRead = inputStream.read(buffer)) != -1)
        // {
        // byteBuffer.put(buffer, 0, bytesRead);
        // }

        while (byteBuffer.position() < contentLength)
        {
            bytesRead = inputStream.read(buffer);

            byteBuffer.put(buffer, 0, Math.min(bytesRead, (int) contentLength - byteBuffer.position()));
        }

        return byteBuffer;
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#supports(java.lang.Class)
     */
    @Override
    protected boolean supports(final Class<?> clazz)
    {
        // return ByteBuffer.class == clazz;
        return ByteBuffer.class.isAssignableFrom(clazz);
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
     */
    @Override
    protected void writeInternal(final ByteBuffer t, final HttpOutputMessage outputMessage) throws IOException
    {
        byte[] buffer = new byte[this.bufferSize];

        while (t.hasRemaining())
        {
            int length = Math.min(t.remaining(), this.bufferSize);

            t.get(buffer, 0, length);

            outputMessage.getBody().write(buffer, 0, length);
        }

        // outputMessage.getBody().flush();
    }
}
