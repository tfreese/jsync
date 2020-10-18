package de.freese.jsync.spring;

// Created: 18.09.2020
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import de.freese.jsync.spring.utils.ByteBufferHttpMessageConverter;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestByteBufferHttpMessageConverter
{
    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testRead() throws Exception
    {
        HttpInputMessage inputMessage = new MockHttpInputMessage(new byte[2050]);
        inputMessage.getHeaders().setContentLength(2050);

        HttpMessageConverter<ByteBuffer> converter = new ByteBufferHttpMessageConverter(2048, () -> ByteBuffer.allocate(4096));
        ByteBuffer byteBuffer = converter.read(ByteBuffer.class, inputMessage);

        byteBuffer.flip();

        assertNotNull(byteBuffer);
        assertEquals(4096, byteBuffer.capacity());
        assertEquals(2050, byteBuffer.limit());
        assertEquals(0, byteBuffer.position());
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void testWrite() throws Exception
    {
        MockHttpOutputMessage httpOutputMessage = new MockHttpOutputMessage();
        ByteBuffer byteBuffer = ByteBuffer.allocate(2050);

        HttpMessageConverter<ByteBuffer> converter = new ByteBufferHttpMessageConverter(2048, () -> ByteBuffer.allocate(4096));
        converter.write(byteBuffer, MediaType.APPLICATION_OCTET_STREAM, httpOutputMessage);

        byte[] bytes = httpOutputMessage.getBodyAsBytes();

        assertNotNull(bytes);
        assertEquals(2050, bytes.length);
    }
}
