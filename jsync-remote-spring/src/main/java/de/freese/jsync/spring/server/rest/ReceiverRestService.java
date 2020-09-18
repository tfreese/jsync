// Created: 15.09.2020
package de.freese.jsync.spring.server.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.ByteBufferInputStream;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "/receiver", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ReceiverRestService
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverRestService.class);

    /**
    *
    */
    @javax.annotation.Resource
    private Receiver receiver;

    /**
     * Erstellt ein neues {@link ReceiverRestService} Object.
     */
    public ReceiverRestService()
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
    @GetMapping("/syncItems")
    public ResponseEntity<Resource> generateSyncItems(@RequestParam("baseDir") final String baseDir,
                                                      @RequestParam("followSymLinks") final boolean followSymLinks, final ServletRequest request,
                                                      final ServletResponse response)
    {
        List<SyncItem> syncItems = new ArrayList<>(128);

        this.receiver.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("{}/{}: SyncItem generated: {}", request.getRemoteAddr(), request.getRemotePort(), syncItem);

            syncItems.add(syncItem);
        });

        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            buffer.putInt(syncItems.size());

            for (SyncItem syncItem : syncItems)
            {
                Serializers.writeTo(buffer, syncItem);
            }

            buffer.flip();

            Resource resource = new InputStreamResource(new ByteBufferInputStream(buffer));

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());
            ResponseEntity<Resource> responseEntity = new ResponseEntity<>(resource, headers, HttpStatus.OK);

            return responseEntity;
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param size long
     * @param resource {@link Resource}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/channel", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> getChannel(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                             @RequestParam("size") final long size, @RequestBody final Resource resource)
    {
        WritableByteChannel channel = this.receiver.getChannel(baseDir, relativeFile, size);

        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            InputStream inputStream = resource.getInputStream();

            byte[] bytes = new byte[8192];
            int bytesRead = 0;

            while ((bytesRead = inputStream.read(bytes)) != -1)
            {
                buffer.clear();
                buffer.put(bytes, 0, bytesRead);
                buffer.flip();

                channel.write(buffer);
            }

            return ResponseEntity.ok("OK");
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
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
     * @param baseDir String
     * @param syncItemData {@link ByteBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> update(@RequestParam("baseDir") final String baseDir, @RequestBody final ByteBuffer syncItemData)
    {
        syncItemData.flip();

        SyncItem syncItem = Serializers.readFrom(syncItemData, SyncItem.class);

        this.receiver.update(baseDir, syncItem);

        return ResponseEntity.ok("OK");
    }

    /**
     * @param baseDir String
     * @param withChecksum boolean
     * @param syncItemData {@link ByteBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/validate", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> validate(@RequestParam("baseDir") final String baseDir, @RequestParam("withChecksum") final boolean withChecksum,
                                           @RequestBody final ByteBuffer syncItemData)
    {
        syncItemData.flip();

        SyncItem syncItem = Serializers.readFrom(syncItemData, SyncItem.class);

        this.receiver.validateFile(baseDir, syncItem, withChecksum);

        return ResponseEntity.ok("OK");
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @param chunk {@link ByteBuffer}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/chunkBuffer", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> writeChunkBuffer(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                   @RequestParam("position") final long position, @RequestParam("size") final long size,
                                                   @RequestBody final ByteBuffer chunk)
    {
        try
        {
            this.receiver.writeChunk(baseDir, relativeFile, position, size, chunk);

            return ResponseEntity.ok("OK");
        }
        finally
        {
            ByteBufferPool.getInstance().release(chunk);
        }
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @param resource {@link Resource}
     * @return {@link ResponseEntity}
     */
    @PostMapping(path = "/chunkStream", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> writeChunkStream(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                   @RequestParam("position") final long position, @RequestParam("size") final long size,
                                                   @RequestBody final Resource resource)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();
        buffer.clear();

        try
        {
            InputStream inputStream = resource.getInputStream();
            byte[] bytes = new byte[8192];
            int bytesRead = 0;

            while ((bytesRead = inputStream.read(bytes)) != -1)
            {
                buffer.put(bytes, 0, bytesRead);
            }

            this.receiver.writeChunk(baseDir, relativeFile, position, size, buffer);

            return ResponseEntity.ok("OK");
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
    }
}
