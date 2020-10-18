// Created: 15.09.2020
package de.freese.jsync.spring.rest.facade;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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
import de.freese.jsync.model.serializer.Serializer;

/**
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "/sender", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class SenderRestFacade
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderRestFacade.class);

    /**
    *
    */
    @javax.annotation.Resource
    private DataBufferFactory dataBufferFactory;

    /**
     *
     */
    @javax.annotation.Resource
    private Sender sender;

    /**
    *
    */
    @javax.annotation.Resource
    private Serializer<DataBuffer> serializer;

    /**
     * Erstellt ein neues {@link SenderRestFacade} Object.
     */
    public SenderRestFacade()
    {
        super();
    }

    /**
     * @return {@link ResponseEntity}
     */
    @GetMapping("/connect")
    public ResponseEntity<String> connect()
    {
        return ResponseEntity.ok("OK");
        // return ResponseEntity.ok().build();
    }

    /**
     * @return {@link ResponseEntity}
     */
    @GetMapping("/disconnect")
    public ResponseEntity<String> disconnect()
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
    public ResponseEntity<DataBuffer> generateSyncItems(@RequestParam("baseDir") final String baseDir,
                                                        @RequestParam("followSymLinks") final boolean followSymLinks, final ServletRequest request,
                                                        final ServletResponse response)
    {
        List<SyncItem> syncItems = new ArrayList<>(128);

        this.sender.generateSyncItems(baseDir, followSymLinks, syncItem -> {
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
        headers.setCacheControl(CacheControl.noCache());
        ResponseEntity<DataBuffer> responseEntity = new ResponseEntity<>(dataBuffer, headers, HttpStatus.OK);

        return responseEntity;
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
        headers.setCacheControl(CacheControl.noCache());
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
     * @return Serializer<DataBuffer>
     */
    private Serializer<DataBuffer> getSerializer()
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
     * @param position long
     * @param sizeOfChunk long
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/chunk/{baseDir}/{relativeFile}/{position}/{size}")
    // public ResponseEntity<Resource> readChunk(@PathVariable("baseDir") final String baseDir, @PathVariable("relativeFile") final String relativeFile,
    // @PathVariable("position") final long position, @PathVariable("size") final long size)
    @GetMapping(path = "/readChunkBuffer", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<DataBuffer> readChunkBuffer(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                      @RequestParam("position") final long position, @RequestParam("sizeOfChunk") final long sizeOfChunk)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) sizeOfChunk);
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);
        this.sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        ResponseEntity<DataBuffer> responseEntity = new ResponseEntity<>(dataBuffer, headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @return {@link ResponseEntity}
     */
    @GetMapping(path = "/readChunkStream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> readChunkStream(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                    @RequestParam("position") final long position, @RequestParam("sizeOfChunk") final long sizeOfChunk)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) sizeOfChunk);
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);
        this.sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);

        Resource resource = new InputStreamResource(dataBuffer.asInputStream(true));

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        ResponseEntity<Resource> responseEntity = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        return responseEntity;
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @return {@link ResponseEntity}
     */
    // @GetMapping("/channel/{baseDir}/{relativeFile}/{position}/{size}")
    @GetMapping(path = "/resourceReadable", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> resourceReadable(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                     @RequestParam("sizeOfFile") final long sizeOfFile)
    {
        Resource resource = this.sender.getResource(baseDir, relativeFile, sizeOfFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        ResponseEntity<Resource> responseEntity = new ResponseEntity<>(resource, headers, HttpStatus.OK);

        return responseEntity;
    }
}
