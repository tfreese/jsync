// Created: 14.10.2020
package de.freese.jsync.spring.webflux.filesystem;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.fileHandle.FileHandle;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.spring.webflux.filesystem.fileHandle.FileHandleFluxDataBuffer;
import de.freese.jsync.spring.webflux.utils.DataBufferAdapter;
import io.netty.channel.ChannelOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * @author Thomas Freese
 */
public class RemoteSenderWebFluxClient extends AbstractSender
{
    /**
     *
     */
    private ConnectionProvider connectionProvider;

    /**
    *
    */
    private final Serializer<DataBuffer> serializer = DefaultSerializer.of(new DataBufferAdapter());

    /**
     *
     */
    private WebClient webClient;

    /**
     * Erstellt ein neues {@link RemoteSenderWebFluxClient} Object.
     */
    public RemoteSenderWebFluxClient()
    {
        super();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.connectionProvider = ConnectionProvider.builder("connection-pool").fifo().maxConnections(16).build();

        LoopResources loopResources = LoopResources.create("client-sender", 2, 4, true);

        // ReactorResourceFactory resourceFactory = new ReactorResourceFactory();
        // resourceFactory.setUseGlobalResources(true);
        // resourceFactory.setConnectionProvider(connectionProvider);
        // resourceFactory.setLoopResourcesSupplier(() -> loopResources);

        // @formatter:off
        HttpClient httpClient=  HttpClient.create(this.connectionProvider)
                //.baseUrl(rootUri)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120_000)
                //.option(ChannelOption.SO_TIMEOUT, 120_000)
                .option(ChannelOption.SO_KEEPALIVE, true)
//                .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true) {
//
//                    /**
//                     * @see io.netty.buffer.PooledByteBufAllocator#newDirectBuffer(int, int)
//                     */
//                    @Override
//                    protected ByteBuf newDirectBuffer(final int initialCapacity, final int maxCapacity)
//                    {
//                        return super.newDirectBuffer(Options.DATABUFFER_SIZE, maxCapacity);
//                    }
//
//                    /**
//                     * @see io.netty.buffer.PooledByteBufAllocator#newHeapBuffer(int, int)
//                     */
//                    @Override
//                    protected ByteBuf newHeapBuffer(final int initialCapacity, final int maxCapacity)
//                    {
//                        return super.newHeapBuffer(Options.DATABUFFER_SIZE, maxCapacity);
//                    }
//
//                })
                .runOn(loopResources)
//                .doOnConnected(connection -> {
//                    connection.addHandlerLast(new ReadTimeoutHandler(120_000, TimeUnit.MILLISECONDS));
//                    connection.addHandlerLast(new WriteTimeoutHandler(120_000, TimeUnit.MILLISECONDS));
//                })
                .responseTimeout(Duration.ofMillis(120_000))
                //.protocol(HttpProtocol.H2)
                ;
        // @formatter:on

        ClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(httpClient);
        // ClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(resourceFactory, Function.identity());

        String rootUri = String.format("http://%s:%d/jsync/sender", uri.getHost(), uri.getPort());

        // @formatter:off
        this.webClient = WebClient.builder()
                .baseUrl(rootUri)
                .clientConnector(clientHttpConnector)
                .codecs(configurer-> {
                    configurer.registerDefaults(true);
                    configurer.defaultCodecs().maxInMemorySize(Options.DATABUFFER_SIZE);
                    //CodecConfigurer.CustomCodecs customCodecs = configurer.customCodecs();

                    //customCodecs.register(new DataBufferEncoder());
                    //customCodecs.register(new DataBufferDecoder());
                 })
//                .exchangeStrategies(builder -> {
//                    return builder.codecs(codecConfigurer -> {
//
//                    });
//                })
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                ;
        // @formatter:on

        // ExchangeStrategies strategies = ExchangeStrategies.builder()
        // .codecs(configurer -> {
        // configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(this.objectMapper, MediaType.APPLICATION_JSON));
        // configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(this.objectMapper, MediaType.APPLICATION_JSON));
        //
        // }).build();

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // this.restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(rootUri, this.restTemplate.getUriTemplateHandler()));

        // @formatter:off
        Mono<String> response = this.webClient
                .get()
                .uri("/connect")
                .retrieve()
                .bodyToMono(String.class)
                ;
        // @formatter:on
        response.block();

//        // @formatter:off
//        Mono<ResponseEntity<String>> response = this.webClient
//              .get()
//              .uri("/connect")
//              .accept(MediaType.APPLICATION_JSON)
//              .exchange()
//              .flatMap(clientResponse -> clientResponse.toEntity(String.class))
//              ;
//        // @formatter:on
        //
        // ResponseEntity<String> responseEntity = response.block();
        // responseEntity.getBody();
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
            //.doOnNext(DataBufferUtils.releaseConsumer())
//            .subscribe(dataBuffer -> {
//                System.err.println(Thread.currentThread().getName());
//                @SuppressWarnings("unused")
//                int itemCount = getSerializer().readFrom(dataBuffer, int.class);
//
//                while (dataBuffer.readableByteCount() > 0)
//                {
//                    SyncItem syncItem = getSerializer().readFrom(dataBuffer, SyncItem.class);
//                    consumerSyncItem.accept(syncItem);
//                }
//
//                DataBufferUtils.release(dataBuffer);
//            })
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
        // DataBufferUtils.retain(dataBuffer);
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

    // /**
    // * @see de.freese.jsync.filesystem.FileSystem#getResource(java.lang.String, java.lang.String, long)
    // */
    // @Override
    // public Resource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    // {
//        // @formatter:off
//        UriComponents builder = UriComponentsBuilder.fromPath("/resourceReadable")
//                .queryParam("baseDir", baseDir)
//                .queryParam("relativeFile", relativeFile)
//                .queryParam("sizeOfFile", sizeOfFile)
//                .build();
//        // @formatter:on
    //
//        // @formatter:off
//        Flux<DataBuffer> response = this.webClient
//                .get()
//                .uri(builder.toUriString())
//                .accept(MediaType.APPLICATION_OCTET_STREAM)
//                .retrieve()
//                .bodyToFlux(DataBuffer.class)
//                ;
//        // @formatter:on
    //
    // try
    // {
    // PipedOutputStream outPipe = new PipedOutputStream();
    // PipedInputStream inPipe = new PipedInputStream(outPipe);
    //
    // DataBufferUtils.write(response, outPipe).subscribe(DataBufferUtils.releaseConsumer());
    //
    // RemoteSenderResource senderResource = new RemoteSenderResource(relativeFile, sizeOfFile, Channels.newChannel(inPipe));
    //
    // return senderResource;
    // }
    // catch (IOException ex)
    // {
    // throw new UncheckedIOException(ex);
    // }
    // }

    /**
     * @return {@link Serializer}<DataBuffer>
     */
    private Serializer<DataBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readFileHandle(java.lang.String, java.lang.String, long)
     */
    @Override
    public FileHandle readFileHandle(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/resourceReadable")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("sizeOfFile", sizeOfFile)
                .build();
        // @formatter:on

        // @formatter:off
        Flux<DataBuffer> response = this.webClient
                .get()
                .uri(builder.toUriString())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                ;
        // @formatter:on

        return new FileHandleFluxDataBuffer(response);
    }

    // /**
    // * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
    // */
    // @Override
    // public void readChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    // {
//        // @formatter:off
//        UriComponents builder = UriComponentsBuilder.fromPath("/readChunkBuffer")
//                .queryParam("baseDir", baseDir)
//                .queryParam("relativeFile", relativeFile)
//                .queryParam("position", position)
//                .queryParam("sizeOfChunk", sizeOfChunk)
//                .build();
//        // @formatter:on
    //
//        // @formatter:off
//        Mono<DataBuffer> response = this.webClient
//                .get()
//                .uri(builder.toUriString())
//                .accept(MediaType.APPLICATION_OCTET_STREAM)
//                .retrieve()
//                .bodyToMono(DataBuffer.class)
//                ;
//        // @formatter:on
    // DataBuffer dataBuffer = response.block();
    //
    // byteBuffer.clear();
    // byteBuffer.put(dataBuffer.asByteBuffer(0, (int) sizeOfChunk));
    // }
}
