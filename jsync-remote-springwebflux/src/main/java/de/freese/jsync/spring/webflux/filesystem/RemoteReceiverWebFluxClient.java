// Created: 17.10.2020
package de.freese.jsync.spring.webflux.filesystem;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.FileResource;
import de.freese.jsync.filesystem.RemoteReceiverResource;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.JsyncThreadFactory;
import de.freese.jsync.utils.buffer.DataBufferAdapter;
import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * @author Thomas Freese
 */
public class RemoteReceiverWebFluxClient extends AbstractReceiver
{
    /**
    *
    */
    private ConnectionProvider connectionProvider;

    /**
    *
    */
    private final DataBufferFactory dataBufferFactory = JSyncUtils.getDataBufferFactory();

    /**
    *
    */
    private final ExecutorService executorService;

    /**
    *
    */
    private final Serializer<DataBuffer> serializer = DefaultSerializer.of(new DataBufferAdapter());

    /**
    *
    */
    private WebClient webClient;

    /**
     * Erstellt ein neues {@link RemoteReceiverWebFluxClient} Object.
     */
    public RemoteReceiverWebFluxClient()
    {
        super();

        this.executorService = Executors.newSingleThreadExecutor(new JsyncThreadFactory("pipe-"));
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.connectionProvider = ConnectionProvider.builder("connection-pool").fifo().maxConnections(16).build();

        LoopResources loopResources = LoopResources.create("client-receiver", 2, 4, true);

        // @formatter:off
        HttpClient httpClient=  HttpClient.create(this.connectionProvider)
                //.baseUrl(rootUri)
                .tcpConfiguration(tcpClient -> {
                    tcpClient = tcpClient.option(ChannelOption.TCP_NODELAY, true);
                    tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120_000);
                    tcpClient = tcpClient.option(ChannelOption.SO_TIMEOUT, 120_000);
                    tcpClient = tcpClient.option(ChannelOption.SO_KEEPALIVE, true);
                    tcpClient = tcpClient.runOn(loopResources);

                    return tcpClient;
                })
                .responseTimeout(Duration.ofMillis(120_000))
                ;
        // @formatter:on

        ClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(httpClient);

        String rootUri = String.format("http://%s:%d/jsync/receiver", uri.getHost(), uri.getPort());

        // @formatter:off
        this.webClient = WebClient.builder()
                .baseUrl(rootUri)
                .clientConnector(clientHttpConnector)
                .codecs(configurer-> {
                    configurer.registerDefaults(true);
                    configurer.defaultCodecs().maxInMemorySize(Options.DATABUFFER_SIZE);
                 })
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                ;
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .get()
                .uri("/connect")
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/createDirectory")
                .queryParam("baseDir", baseDir)
                .queryParam("relativePath", relativePath)
                .build();
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .get()
                .uri(builder.toUriString())
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/delete")
                .queryParam("baseDir", baseDir)
                .queryParam("relativePath", relativePath)
                .queryParam("followSymLinks", followSymLinks)
                .build();
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .get()
                .uri(builder.toUriString())
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        Mono<String> response = this.webClient.get().uri("/disconnect").retrieve().bodyToMono(String.class);
        response.block(); // .subscribe(response -> this.connectionProvider.dispose());

        this.connectionProvider.dispose();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/syncItems")
                .queryParam("baseDir", baseDir)
                .queryParam("followSymLinks", followSymLinks)
                .build();
        // @formatter:on

        // @formatter:off
        Mono<DataBuffer> response = this.webClient
            .get()
            .uri(builder.toUriString())
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToMono(DataBuffer.class)
        ;
        // @formatter:on

        DataBuffer dataBuffer = response.block();

        @SuppressWarnings("unused")
        int itemCount = getSerializer().readFrom(dataBuffer, int.class);

        while (dataBuffer.readableByteCount() > 0)
        {
            SyncItem syncItem = getSerializer().readFrom(dataBuffer, SyncItem.class);
            consumerSyncItem.accept(syncItem);
        }

        DataBufferUtils.release(dataBuffer);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/checksum")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .build();
        // @formatter:on

        Mono<String> response = this.webClient.get().uri(builder.toUriString()).retrieve().bodyToMono(String.class);
        String checksum = response.block();

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getResource(java.lang.String, java.lang.String, long)
     */
    @Override
    public WritableResource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/resourceWritable")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("sizeOfFile", sizeOfFile)
                .build();
        // @formatter:on

        try
        {
            PipedOutputStream pipeOut = new PipedOutputStream();
            PipedInputStream pipeIn = new PipedInputStream(pipeOut, 8192);

            Callable<String> callable = () -> {
                // @formatter:off
                Mono<String> response = this.webClient
                        .post()
                        .uri(builder.toUriString())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(BodyInserters.fromResource(new InputStreamResource(pipeIn)))
                        .retrieve()
                        .bodyToMono(String.class)
                        ;
                // @formatter:on

                return response.block();
            };

            final Future<String> future = this.executorService.submit(callable);

            WritableByteChannel channel = new WritableByteChannel()
            {
                /**
                 *
                 */
                private final byte[] bytes = new byte[Options.BUFFER_SIZE];

                /**
                 * @see java.nio.channels.Channel#close()
                 */
                @Override
                public void close() throws IOException
                {
                    pipeOut.flush();
                    pipeOut.close();

                    try
                    {
                        future.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        getLogger().error(null, ex);
                    }

                    pipeIn.close();
                }

                /**
                 * @see java.nio.channels.Channel#isOpen()
                 */
                @Override
                public boolean isOpen()
                {
                    return true;
                }

                /**
                 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
                 */
                @Override
                public int write(final ByteBuffer src) throws IOException
                {
                    int length = Math.min(src.remaining(), this.bytes.length);

                    src.get(this.bytes, 0, length);

                    pipeOut.write(this.bytes, 0, length);

                    return length;
                }
            };

            return new RemoteReceiverResource(baseDir + "/" + relativeFile, sizeOfFile, channel);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @return {@link Serializer}<DataBuffer>
     */
    private Serializer<DataBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        getSerializer().writeTo(dataBuffer, syncItem);

        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/update")
                .queryParam("baseDir", baseDir)
                .build()
                ;
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .post()
                .uri(builder.toUriString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromDataBuffers(Mono.just(dataBuffer)))
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer();
        dataBuffer.readPosition(0);
        dataBuffer.writePosition(0);

        getSerializer().writeTo(dataBuffer, syncItem);

        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/validate")
                .queryParam("baseDir", baseDir)
                .queryParam("withChecksum", withChecksum)
                .build()
                ;
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .post()
                .uri(builder.toUriString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromDataBuffers(Mono.just(dataBuffer)))
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void writeChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/writeChunkBuffer")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("position", position)
                .queryParam("sizeOfChunk", sizeOfChunk)
                .build();
        // @formatter:on

        byteBuffer.flip();

        // @formatter:off
        Mono<String> response = this.webClient
                .post()
                .uri(builder.toUriString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(BodyInserters.fromValue(byteBuffer))
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeFileResource(java.lang.String, java.lang.String, long, de.freese.jsync.filesystem.FileResource,
     *      java.util.function.LongConsumer)
     */
    @Override
    public void writeFileResource(final String baseDir, final String relativeFile, final long sizeOfFile, final FileResource fileResource,
                                  final LongConsumer bytesWrittenConsumer)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/resourceWritable")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("sizeOfFile", sizeOfFile)
                .build();
        // @formatter:on

        // @formatter:off
        Mono<String> response = this.webClient
                .post()
                .uri(builder.toUriString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                //.body(BodyInserters.fromResource(new InputStreamResource(pipeIn)))
                .body(BodyInserters.fromValue(fileResource.getReadableByteChannel()))
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on

        response.block();
    }
}
