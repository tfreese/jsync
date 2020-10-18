// Created: 18.10.2020
package de.freese.jsync.spring.webflux.facade;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializer;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
@RestController
@RequestMapping(path = "/sender", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class SenderWebfluxFacade
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(SenderWebfluxFacade.class);

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
     * Erstellt ein neues {@link SenderWebfluxFacade} Object.
     */
    public SenderWebfluxFacade()
    {
        super();
    }

    /**
     * @param uri {@link URI}
     * @return {@link Publisher}
     */
    @GetMapping("/connect")
    // @ResponseStatus(HttpStatus.OK)
    public Publisher<ServerResponse> connect(final URI uri)
    {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("OK"));
    }

    /**
     * @param uri {@link URI}
     * @return {@link Publisher}
     */
    @GetMapping("/disconnect")
    public Publisher<ServerResponse> disconnect(final URI uri)
    {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue("OK"));
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param request {@link ServletRequest}
     * @param response {@link ServletResponse}
     * @return {@link Publisher}
     */
    @GetMapping(path = "/syncItems", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Publisher<DataBuffer> generateSyncItems(@RequestParam("baseDir") final String baseDir, @RequestParam("followSymLinks") final boolean followSymLinks,
                                                   final ServletRequest request, final ServletResponse response)
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

        // HttpHeaders headers = new HttpHeaders();
        // headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        // ResponseEntity<DataBuffer> responseEntity = new ResponseEntity<>(dataBuffer, headers, HttpStatus.OK);
        //
        // return responseEntity;

        return Mono.just(dataBuffer);

//        // @formatter:off
//        return ServerResponse.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
//                .body(BodyInserters.fromValue(dataBuffer))
//                ;
//        // @formatter:on
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @return {@link Publisher}
     */
    @GetMapping("/checksum")
    public Publisher<String> getChecksum(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile)
    {
        String checksum = this.sender.getChecksum(baseDir, relativeFile, i -> {
        });

        // HttpHeaders headers = new HttpHeaders();
        // headers.setCacheControl(CacheControl.noCache());
        // ResponseEntity<String> responseEntity = new ResponseEntity<>(checksum, headers, HttpStatus.OK);

        return Mono.just(checksum);
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

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @return {@link Publisher}
     */
    @GetMapping(path = "/readChunkBuffer", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Publisher<DataBuffer> readChunkBuffer(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                 @RequestParam("position") final long position, @RequestParam("sizeOfChunk") final long sizeOfChunk)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) sizeOfChunk);
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);
        this.sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, byteBuffer);

        // HttpHeaders headers = new HttpHeaders();
        // headers.setCacheControl(CacheControl.noCache());
        // ResponseEntity<DataBuffer> responseEntity = new ResponseEntity<>(dataBuffer, headers, HttpStatus.OK);
        //
        // return responseEntity;

        return Mono.just(dataBuffer);
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @return {@link ResponseEntity}
     */
    @GetMapping(path = "/resourceReadable", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Publisher<Resource> resourceReadable(@RequestParam("baseDir") final String baseDir, @RequestParam("relativeFile") final String relativeFile,
                                                @RequestParam("sizeOfFile") final long sizeOfFile)
    {
        Resource resource = this.sender.getResource(baseDir, relativeFile, sizeOfFile);

        // HttpHeaders headers = new HttpHeaders();
        // headers.setCacheControl(CacheControl.noCache());
        // ResponseEntity<Resource> responseEntity = new ResponseEntity<>(resource, headers, HttpStatus.OK);
        //
        // return responseEntity;
        return Mono.just(resource);
    }
}
