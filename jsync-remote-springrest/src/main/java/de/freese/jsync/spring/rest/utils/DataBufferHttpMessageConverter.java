// Created: 17.09.2020
package de.freese.jsync.spring.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;

/**
 * @author Thomas Freese
 */
public class DataBufferHttpMessageConverter extends AbstractHttpMessageConverter<DataBuffer>
{
    /**
    *
    */
    private final int bufferSize;

    /**
     *
     */
    private final DataBufferFactory dataBufferFactory;

    /**
     * Erstellt ein neues {@link DataBufferHttpMessageConverter} Object.
     *
     * @param bufferSize int
     * @param dataBufferFactory {@link DataBufferFactory}
     */
    public DataBufferHttpMessageConverter(final int bufferSize, final DataBufferFactory dataBufferFactory)
    {
        super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);

        if (bufferSize <= 0)
        {
            throw new IllegalArgumentException("bufferSize <= 0: " + bufferSize);
        }

        this.bufferSize = bufferSize;
        this.dataBufferFactory = Objects.requireNonNull(dataBufferFactory, "dataBufferFactory required");
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#getContentLength(java.lang.Object, org.springframework.http.MediaType)
     */
    @Override
    protected Long getContentLength(final DataBuffer t, final MediaType contentType) throws IOException
    {
        return (long) t.readableByteCount();
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
     */
    @Override
    protected DataBuffer readInternal(final Class<? extends DataBuffer> clazz, final HttpInputMessage inputMessage) throws IOException
    {
        long contentLength = inputMessage.getHeaders().getContentLength();

        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        InputStream inputStream = inputMessage.getBody();
        byte[] buffer = new byte[this.bufferSize];
        int bytesRead = 0;

        while (dataBuffer.writePosition() < contentLength)
        {
            bytesRead = inputStream.read(buffer);

            dataBuffer.write(buffer, 0, Math.min(bytesRead, (int) contentLength - dataBuffer.writePosition()));
        }

        return dataBuffer;
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#supports(java.lang.Class)
     */
    @Override
    protected boolean supports(final Class<?> clazz)
    {
        return DataBuffer.class.isAssignableFrom(clazz);
    }

    /**
     * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
     */
    @Override
    protected void writeInternal(final DataBuffer t, final HttpOutputMessage outputMessage) throws IOException
    {
        byte[] buffer = new byte[this.bufferSize];

        while (t.readableByteCount() > 0)
        {
            int length = Math.min(t.readableByteCount(), this.bufferSize);

            t.read(buffer, 0, length);

            outputMessage.getBody().write(buffer, 0, length);
        }

        // outputMessage.getBody().flush();
        DataBufferUtils.release(t);
    }
}
