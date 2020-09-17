// Created: 17.09.2020
package de.freese.jsync.spring.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class ByteBufferHttpMessageConverter extends AbstractHttpMessageConverter<ByteBuffer>
{
    /**
     *
     */
    public static final int BUFFER_SIZE = 4096;

    /**
     * Erstellt ein neues {@link ByteBufferHttpMessageConverter} Object.
     */
    public ByteBufferHttpMessageConverter()
    {
        super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
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
    protected ByteBuffer readInternal(final Class<? extends ByteBuffer> clazz, final HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException
    {
        long contentLength = inputMessage.getHeaders().getContentLength();
        // ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength);

        ByteBuffer byteBuffer = ByteBufferPool.getInstance().get();
        byteBuffer.clear();

        int bytesRead = 0;
        byte[] buffer = new byte[BUFFER_SIZE];

        // while ((bytesRead = inputMessage.getBody().read(buffer)) != -1)
        while (byteBuffer.position() < contentLength)
        {
            bytesRead = inputMessage.getBody().read(buffer);

            byteBuffer.put(buffer, 0, Math.min(bytesRead, (int) contentLength - byteBuffer.position()));
        }

        byteBuffer.flip();

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
    protected void writeInternal(final ByteBuffer t, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException
    {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (t.hasRemaining())
        {
            int length = Math.min(t.remaining(), BUFFER_SIZE);

            t.get(buffer, 0, length);

            outputMessage.getBody().write(buffer);
        }

        outputMessage.getBody().flush();
    }
}
