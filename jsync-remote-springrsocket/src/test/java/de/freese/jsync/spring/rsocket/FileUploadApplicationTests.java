// Created: 18.10.2020
package de.freese.jsync.spring.rsocket;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.MimeType;
import de.freese.jsync.Options;
import de.freese.jsync.spring.rsocket.model.Constants;
import de.freese.jsync.spring.rsocket.model.Status;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
@SpringBootTest(classes = JsyncRSocketApplication.class)
@ActiveProfiles("test")
class FileUploadApplicationTests
{
    // /**
    // * @throws Exception Falls was schief geht.
    // */
    // @AfterAll
    // static void afterAll() throws Exception
    // {
    // Thread.sleep(10_000);
    // }

    /**
     *
     */
    @javax.annotation.Resource
    private RSocketRequester rSocketRequester;

    /**
     *
     */
    @Test
    void uploadFile()
    {
        Resource resource = new FileSystemResource(Paths.get(System.getProperty("user.home"), "downloads", "iso", "archlinux-2020.10.01-x86_64.iso"));

        // DataBufferFactory dataBufferFactory = new DefaultPooledDataBufferFactory();
        // DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
        DataBufferFactory dataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);

        Flux<DataBuffer> readFlux = DataBufferUtils.read(resource, dataBufferFactory, Options.DATABUFFER_SIZE).doOnNext(s -> System.out.println("Sent"));

        // @formatter:off
//        this.rSocketRequester
//            .map(r -> r.route("file.upload")
//                    .metadata(metadataSpec -> {
//                        metadataSpec.metadata("data", MimeType.valueOf(Constants.MIME_FILE_EXTENSION));
//                        metadataSpec.metadata("output", MimeType.valueOf(Constants.MIME_FILE_NAME));
//                    })
//            .data(readFlux))
//            .flatMapMany(r -> r.retrieveFlux(Status.class))
//            .doOnNext(s -> System.out.printf("Upload Status: %s - %s%n", Thread.currentThread().getName(), s))
//            .blockLast()
//            //.subscribe()
//            ;
        this.rSocketRequester
            .route("file.upload")
            .metadata(metadataSpec -> {
                metadataSpec.metadata("output", MimeType.valueOf(Constants.MIME_FILE_NAME));
                metadataSpec.metadata("data", MimeType.valueOf(Constants.MIME_FILE_EXTENSION));
            })
            .data(readFlux)
            .retrieveFlux(Status.class)
            .doOnNext(s -> System.out.printf("Upload Status: %s - %s%n", Thread.currentThread().getName(), s))
            .blockLast()
            ;
        // @formatter:on
    }
}
