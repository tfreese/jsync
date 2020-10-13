// Created: 15.09.2020
package de.freese.jsync.spring.rest.facade;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializer;

/**
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "/receiver", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ReceiverRestFacade
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverRestFacade.class);

    /**
    *
    */
    @javax.annotation.Resource
    private DataBufferFactory dataBufferFactory;

    /**
    *
    */
    @javax.annotation.Resource
    private Receiver receiver;

    /**
    *
    */
    @javax.annotation.Resource
    private Serializer<DataBuffer> serializer;

    /**
     * Erstellt ein neues {@link ReceiverRestFacade} Object.
     */
    public ReceiverRestFacade()
    {
        super();
    }

    /**
     * @param uri {@link URI}
     * @return {@link ResponseEntity}
     */
    @GetMapping("/connect")
    public ResponseEntity<String> connect(final URI uri)
    {
        return ResponseEntity.ok("OK");
    }

    /**
     * @param baseDir String
     * @param relativePath String
     * @return {@link ResponseEntity}
     */
    @GetMapping("/createDirectory")
    public ResponseEntity<String> createDirectory(@RequestParam("baseDir") final String baseDir, @RequestParam("relativePath") final String relativePath)
    {
        this.receiver.createDirectory(baseDir, relativePath);

        return ResponseEntity.ok("OK");
    }

    /**
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     * @return {@link ResponseEntity}
     */
    @GetMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam("baseDir") final String baseDir, @RequestParam("relativePath") final String relativePath,
                                         @RequestParam("followSymLinks") final boolean followSymLinks)
    {
        this.receiver.delete(baseDir, relativePath, followSymLinks);

        return ResponseEntity.ok("OK");
    }

    /**
     * @param uri {@link URI}
     * @return {@link ResponseEntity}
     */
    @GetMapping("/disconnect")
    public ResponseEntity<String> disconnect(final URI uri)
    {
        return ResponseEntity.ok("OK");
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param request {@link ServletRequest}
     * @param response {@link ServletResponse}
     * @return {@link ResponseEntity}
     */
    @GetMapping(path = "/syncItems", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<DataBuffer> generateSyncItems(@RequestParam("baseDir") final String baseDir,
                                                        @RequestParam("followSymLinks") final boolean followSymLinks, final ServletRequest request,
                                                        final ServletResponse response)
    {
        List<SyncItem> syncItems = new ArrayList<>(128);

        this.receiver.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("{}/{}: SyncItem generated: {}", request.getRemoteAddr(), request.getRemotePort(), syncItem);

            syncItems.add(syncItem);
        });

        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        getSerializer().writeTo(dataBuffer, syncItems.size());

        for (SyncItem syncItem : syncItems)
        {
            getSerializer().writeTo(dataBuffer, syncItem);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        ResponseEntity<DataBuffer> responseEntity = new ResponseEntity<>(dataBuffer, headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @return {@link ResponseEntity}
     */
    @GetMapping("/checksum")
    public ResponseEntity<String> getChecksum(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile)
    {
        String checksum = this.receiver.getChecksum(baseDir, relativeFile, i -> {
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        ResponseEntity<String> responseEntity = new ResponseEntity<>(checksum, headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @return {@link Serializer}<DataBuffer>
     */
    private Serializer<DataBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @param resource {@link Resource}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/resourceWritable", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> resourceWritable(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                   @RequestParam("sizeOfFile") final long sizeOfFile, @RequestBody final Resource resource)
    {
        WritableResource writableResource = this.receiver.getResource(baseDir, relativeFile, sizeOfFile);

        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) Math.min(sizeOfFile, Options.DATABUFFER_SIZE));
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try (ReadableByteChannel readableByteChannel = resource.readableChannel();
             WritableByteChannel writableByteChannel = writableResource.writableChannel())
        {
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());
            long totalRead = 0;

            while (totalRead < sizeOfFile)
            {
                byteBuffer.clear();

                int bytesRead = readableByteChannel.read(byteBuffer);
                totalRead += bytesRead;

                byteBuffer.flip();

                while (byteBuffer.hasRemaining())
                {
                    writableByteChannel.write(byteBuffer);
                }
            }

            return ResponseEntity.ok("OK");
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }

    /**
     * @param baseDir String
     * @param syncItemData {@link DataBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> update(@RequestParam("baseDir") final String baseDir, @RequestBody final DataBuffer syncItemData)
    {
        try
        {
            syncItemData.readPosition(0);

            SyncItem syncItem = getSerializer().readFrom(syncItemData, SyncItem.class);

            this.receiver.update(baseDir, syncItem);

            return ResponseEntity.ok("OK");
        }
        finally
        {
            DataBufferUtils.release(syncItemData);
        }
    }

    /**
     * @param baseDir String
     * @param withChecksum boolean
     * @param syncItemData {@link DataBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/validate", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> validate(@RequestParam("baseDir") final String baseDir, @RequestParam("withChecksum") final boolean withChecksum,
                                           @RequestBody final DataBuffer syncItemData)
    {
        try
        {
            syncItemData.readPosition(0);

            SyncItem syncItem = getSerializer().readFrom(syncItemData, SyncItem.class);

            this.receiver.validateFile(baseDir, syncItem, withChecksum);

            return ResponseEntity.ok("OK");
        }
        finally
        {
            DataBufferUtils.release(syncItemData);
        }
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param chunk {@link DataBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/writeChunkBuffer", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> writeChunkBuffer(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                   @RequestParam("position") final long position, @RequestParam("sizeOfChunk") final long sizeOfChunk,
                                                   @RequestBody final DataBuffer chunk)
    {
        try
        {
            chunk.readPosition(0);

            ByteBuffer byteBuffer = chunk.asByteBuffer(0, (int) sizeOfChunk);

            this.receiver.writeChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);

            return ResponseEntity.ok("OK");
        }
        finally
        {
            DataBufferUtils.release(chunk);
        }
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param resource {@link Resource}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/writeChunkStream", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> writeChunkStream(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                   @RequestParam("position") final long position, @RequestParam("sizeOfChunk") final long sizeOfChunk,
                                                   @RequestBody final Resource resource)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        try
        {
            InputStream inputStream = resource.getInputStream();
            byte[] bytes = new byte[Options.BUFFER_SIZE];
            int bytesRead = 0;

            while ((bytesRead = inputStream.read(bytes)) != -1)
            {
                dataBuffer.write(bytes, 0, bytesRead);
            }

            ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);

            this.receiver.writeChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);

            return ResponseEntity.ok("OK");
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            DataBufferUtils.release(dataBuffer);
        }
    }
}
