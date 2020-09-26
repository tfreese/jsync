// Created: 15.09.2020
package de.freese.jsync.spring.server.rest;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.utils.ByteBufferInputStream;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "/sender", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class SenderRestService
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderRestService.class);

    /**
     *
     */
    @javax.annotation.Resource
    private Sender sender;

    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Erstellt ein neues {@link SenderRestService} Object.
     */
    public SenderRestService()
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
        // return ResponseEntity.ok().build();
    }

    /**
     * @param uri {@link URI}
     * @return {@link ResponseEntity}
     */
    @GetMapping("/disconnect")
    public ResponseEntity<String> disconnect(final URI uri)
    {
        return ResponseEntity.ok("OK");
        // return ResponseEntity.ok().build();
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param request {@link ServletRequest}
     * @param response {@link ServletResponse}
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/syncItems/{baseDir}/{followSymLinks}")
    // public ResponseEntity<Resource> generateSyncItems(@PathVariable("baseDir") final String baseDir,
    // @PathVariable("followSymLinks") final boolean followSymLinks, final HttpServletRequest request,
    // final HttpServletResponse response)
    @GetMapping(path = "/syncItems", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ByteBuffer> generateSyncItems(@RequestParam("baseDir") final String baseDir,
                                                        @RequestParam("followSymLinks") final boolean followSymLinks, final ServletRequest request,
                                                        final ServletResponse response)
    {
        List<SyncItem> syncItems = new ArrayList<>(128);

        this.sender.generateSyncItems(baseDir, followSymLinks, syncItem -> {
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
                getSerializer().writeTo(buffer, syncItem);
            }

            buffer.flip();

            // byte[] data = new byte[buffer.limit()];
            // buffer.get(data);
            // Resource resource = new InputStreamResource(new ByteBufferInputStream(buffer));

            // response.getOutputStream().write(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());
            ResponseEntity<ByteBuffer> responseEntity = new ResponseEntity<>(buffer, headers, HttpStatus.OK);

            return responseEntity;
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    // @RequestMapping(value = "/image-byte-array", method = RequestMethod.GET)
    // public @ResponseBody byte[] getImageAsByteArray() throws IOException {
    // InputStream in = servletContext.getResourceAsStream("/WEB-INF/images/image-example.jpg");
    // return IOUtils.toByteArray(in);
    // }

    // @ResponseBody
    // @RequestMapping(value = "/image-resource", method = RequestMethod.GET)
    // public Resource getImageAsResource() {
    // return new ServletContextResource(servletContext, "/WEB-INF/images/image-example.jpg");
    // }

    // @RequestMapping(value = "/image-resource", method = RequestMethod.GET)
    // @ResponseBody
    // public ResponseEntity<Resource> getImageAsResource() {
    // HttpHeaders headers = new HttpHeaders();
    // Resource resource =
    // new ServletContextResource(servletContext, "/WEB-INF/images/image-example.jpg");
    // return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    // }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/checksum/{baseDir}/{relativeFile}")
    // public ResponseEntity<String> getChecksum(@PathVariable("baseDir") final String baseDir, @PathVariable("relativeFile") final String relativeFile)
    @GetMapping("/checksum")
    public ResponseEntity<String> getChecksum(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile)
    {
        String checksum = this.sender.getChecksum(baseDir, relativeFile, i -> {
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
     * @return Serializer<ByteBuffer>
     */
    private Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    // @RequestMapping(value = "/submissions/signature/{type}/{id}",
    // method = RequestMethod.GET)
    // public HttpEntity getFile(HttpServletResponse response,
    // @PathVariable String type,
    // @PathVariable Integer id) {
    // String base64 = "foo"; // get base-64 encoded string from db
    // byte[] bytes = Base64.decodeBase64(base64);
    // try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
    // StreamUtils.copy(inputStream, response.getOutputStream());
    // response.setContentType(MediaType.IMAGE_PNG_VALUE);
    // } catch (IOException e) {
    // // handle
    // }
    // return new ResponseEntity(HttpStatus.OK);
    // }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param size long
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/channel/{baseDir}/{relativeFile}/{position}/{size}")
    @GetMapping("/readChannel")
    public ResponseEntity<Resource> readChannel(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                @RequestParam("size") final long size)
    {
        ReadableByteChannel channel = this.sender.getChannel(baseDir, relativeFile, size);

        Resource resource = new InputStreamResource(Channels.newInputStream(channel));

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        ResponseEntity<Resource> responseEntity = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param size long
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/chunk/{baseDir}/{relativeFile}/{position}/{size}")
    // public ResponseEntity<Resource> readChunk(@PathVariable("baseDir") final String baseDir, @PathVariable("relativeFile") final String relativeFile,
    // @PathVariable("position") final long position, @PathVariable("size") final long size)
    @GetMapping(path = "/readChunkBuffer", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ByteBuffer> readChunkBuffer(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                      @RequestParam("position") final long position, @RequestParam("size") final long size)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            this.sender.readChunk(baseDir, relativeFile, position, size, buffer);
            buffer.flip();

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());
            ResponseEntity<ByteBuffer> responseEntity = new ResponseEntity<>(buffer, headers, HttpStatus.OK);

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
     * @param position long
     * @param size long
     * @return {@link ResponseEntity}
     */
    @GetMapping(path = "/readChunkStream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> readChunkStream(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                    @RequestParam("position") final long position, @RequestParam("size") final long size)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            buffer.clear();
            this.sender.readChunk(baseDir, relativeFile, position, size, buffer);
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
}
