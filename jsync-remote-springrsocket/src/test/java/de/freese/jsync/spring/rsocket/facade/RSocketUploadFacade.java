// Created: 18.10.2020
package de.freese.jsync.spring.rsocket.facade;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import de.freese.jsync.spring.rsocket.model.Constants;
import de.freese.jsync.spring.rsocket.model.Status;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Thomas Freese
 */
@Controller
public class RSocketUploadFacade
{
    /**
     * @param metadata {@link Map}
     * @param content {@link Flux}
     * @return {@link Flux}
     * @throws IOException Falls was schief geht.
     */
    @MessageMapping("file.upload")
    public Flux<Status> upload(@Headers final Map<String, Object> metadata, @Payload final Flux<DataBuffer> content) throws IOException
    {
        var fileName = metadata.get(Constants.FILE_NAME);
        var fileExtn = metadata.get(Constants.FILE_EXTENSION);

        var path = Paths.get(fileName + "." + fileExtn);

        return Flux.concat(uploadFile(path, content), Mono.just(Status.COMPLETED)).onErrorReturn(Status.FAILED);
    }

    /**
     * @param path {@link Path}
     * @param bufferFlux {@link Flux}
     * @return {@link Flux}
     * @throws IOException Falls was schief geht.
     */
    private Flux<Status> uploadFile(final Path path, final Flux<DataBuffer> bufferFlux) throws IOException
    {
        Path outputPath = Paths.get(System.getProperty("java.io.tmpdir"));
        Path opPath = outputPath.resolve(path);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(opPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        return DataBufferUtils.write(bufferFlux, channel).map(b -> Status.CHUNK_COMPLETED);
        // bufferFlux.delayElements(Duration.ofSeconds(1)
    }
}
