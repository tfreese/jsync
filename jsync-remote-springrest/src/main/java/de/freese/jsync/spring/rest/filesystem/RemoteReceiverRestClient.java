// Created: 15.09.2020
package de.freese.jsync.spring.rest.filesystem;

import java.net.URI;
import java.nio.channels.Channels;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.fileHandle.FileHandle;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.spring.rest.utils.DataBufferHttpMessageConverter;
import de.freese.jsync.spring.rest.utils.HttpHeaderInterceptor;
import de.freese.jsync.spring.rest.utils.buffer.DataBufferAdapter;
import de.freese.jsync.spring.rest.utils.buffer.DefaultPooledDataBufferFactory;
import de.freese.jsync.utils.io.MonitoringReadableByteChannel;

/**
 * {@link Receiver} für Remote-Filesysteme für Spring-REST.
 *
 * @author Thomas Freese
 */
public class RemoteReceiverRestClient extends AbstractReceiver
{
    /**
    *
    */
    private final DataBufferFactory dataBufferFactory = DefaultPooledDataBufferFactory.getInstance();

    // /**
    // *
    // */
    // private final ExecutorService executorService;

    /**
    *
    */
    private PoolingHttpClientConnectionManager poolingConnectionManager;

    /**
    *
    */
    private RestTemplate restTemplate;

    /**
    *
    */
    private RestTemplateBuilder restTemplateBuilder;

    /**
    *
    */
    private final Serializer<DataBuffer> serializer = DefaultSerializer.of(new DataBufferAdapter());

    /**
     * Erstellt ein neues {@link RemoteReceiverRestClient} Object.
     */
    public RemoteReceiverRestClient()
    {
        super();

        // this.executorService = Executors.newSingleThreadExecutor(new JsyncThreadFactory("pipe-"));
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // @formatter:off
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
//                .register("https", new SSLConnectionSocketFactory(SSLContexts.createDefault(), new NoopHostnameVerifier()))
                .build()
                ;
        // @formatter:on

        this.poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        this.poolingConnectionManager.setMaxTotal(30);
        this.poolingConnectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(1200_00).build());

        // @formatter:off
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(120_000)
                .setConnectTimeout(120_000)
                .setSocketTimeout(120_000).build()
                ;

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(this.poolingConnectionManager)
                .setUserAgent("JSync")
                .build()
                ;
        // @formatter:on

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        httpRequestFactory.setBufferRequestBody(false); // Streaming

        // String rootUri = String.format("http://%s:%d/jsync/receiver", uri.getHost(), uri.getPort());
        String rootUri = String.format("http://%s:%d/receiver", uri.getHost(), uri.getPort());

        // @formatter:off
        this.restTemplateBuilder = new RestTemplateBuilder()
                .rootUri(rootUri)
                .requestFactory(() -> httpRequestFactory)
                .additionalMessageConverters( new ByteArrayHttpMessageConverter()
                        , new StringHttpMessageConverter()
                        , new ResourceHttpMessageConverter(true)
                        //, new ByteBufferHttpMessageConverter(Options.BUFFER_SIZE, () -> ByteBufferPool.getInstance().get())
                        //, new ByteBufferHttpMessageConverter(Options.BUFFER_SIZE, () -> ByteBuffer.allocateDirect(Options.DATABUFFER_SIZE))
                        , new DataBufferHttpMessageConverter(Options.BUFFER_SIZE, getDataBufferFactory())
                        )
                ;
        // @formatter:on

        // @formatter:off
        this.restTemplate = this.restTemplateBuilder
                .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_JSON_VALUE),new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
                .build()
                ;
        // @formatter:on);

        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity("/connect", String.class);
        responseEntity.getBody();
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

        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), String.class);
        responseEntity.getBody();
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

        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), String.class);
        responseEntity.getBody();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity("/disconnect", String.class);
        responseEntity.getBody();

        this.poolingConnectionManager.close();

        // JSyncUtils.shutdown(this.executorService, getLogger());
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

        ResponseEntity<DataBuffer> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), DataBuffer.class);

        DataBuffer bufferResponse = responseEntity.getBody();

        try
        {
            @SuppressWarnings("unused")
            int itemCount = getSerializer().readFrom(bufferResponse, int.class);

            while (bufferResponse.readableByteCount() > 0)
            {
                SyncItem syncItem = getSerializer().readFrom(bufferResponse, SyncItem.class);
                consumerSyncItem.accept(syncItem);
            }
        }
        finally
        {
            DataBufferUtils.release(bufferResponse);
        }
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

        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), String.class);
        String checksum = responseEntity.getBody();

        return checksum;
    }

    /**
     * @return {@link DataBufferFactory}
     */
    private DataBufferFactory getDataBufferFactory()
    {
        return this.dataBufferFactory;
    }

    // /**
    // * @see de.freese.jsync.filesystem.receiver.Receiver#getResource(java.lang.String, java.lang.String, long)
    // */
    // @Override
    // public WritableResource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    // {
//        // @formatter:off
//        UriComponents builder = UriComponentsBuilder.fromPath("/resourceWritable")
//                .queryParam("baseDir", baseDir)
//                .queryParam("relativeFile", relativeFile)
//                .queryParam("sizeOfFile", sizeOfFile)
//                .build();
//         // @formatter:on
    //
//        // @formatter:off
//        RestTemplate rt = this.restTemplateBuilder
//            .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE), new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
//            .build()
//            ;
//        // @formatter:on
    //
    // try
    // {
    // PipedOutputStream pipeOut = new PipedOutputStream();
    // PipedInputStream pipeIn = new PipedInputStream(pipeOut, Options.BUFFER_SIZE);
    //
    // Callable<String> callable = () -> {
    // Resource resource = new InputStreamResource(pipeIn);
    // ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), resource, String.class);
    // return responseEntity.getBody();
    // };
    //
    // final Future<String> future = this.executorService.submit(callable);
    //
    // WritableByteChannel channel = new WritableByteChannel()
    // {
    // /**
    // *
    // */
    // private final byte[] bytes = new byte[Options.BUFFER_SIZE];
    //
    // /**
    // * @see java.nio.channels.Channel#close()
    // */
    // @Override
    // public void close() throws IOException
    // {
    // pipeOut.flush();
    // pipeOut.close();
    //
    // try
    // {
    // future.get();
    // }
    // catch (InterruptedException | ExecutionException ex)
    // {
    // getLogger().error(null, ex);
    // }
    //
    // pipeIn.close();
    // }
    //
    // /**
    // * @see java.nio.channels.Channel#isOpen()
    // */
    // @Override
    // public boolean isOpen()
    // {
    // return true;
    // }
    //
    // /**
    // * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
    // */
    // @Override
    // public int write(final ByteBuffer src) throws IOException
    // {
    // int length = Math.min(src.remaining(), this.bytes.length);
    //
    // src.get(this.bytes, 0, length);
    //
    // pipeOut.write(this.bytes, 0, length);
    //
    // return length;
    // }
    // };
    //
    // return new RemoteReceiverResource(baseDir + "/" + relativeFile, sizeOfFile, channel);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        DataBuffer buffer = getDataBufferFactory().allocateBuffer();
        buffer.readPosition(0).writePosition(0);

        getSerializer().writeTo(buffer, syncItem);

        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/update")
                .queryParam("baseDir", baseDir)
                .build()
                ;
        // @formatter:on

        // @formatter:off
        RestTemplate rt = this.restTemplateBuilder
                .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE), new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
                .build()
                ;
        // @formatter:on

        ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), buffer, String.class);
        responseEntity.getBody();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        DataBuffer dataBuffer = getDataBufferFactory().allocateBuffer();
        dataBuffer.readPosition(0).writePosition(0);

        getSerializer().writeTo(dataBuffer, syncItem);

        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/validate")
                .queryParam("baseDir", baseDir)
                .queryParam("withChecksum", withChecksum)
                .build()
                ;
        // @formatter:on

        // @formatter:off
        RestTemplate rt = this.restTemplateBuilder
                .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE), new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
                .build()
                ;
        // @formatter:on

        ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), dataBuffer, String.class);
        responseEntity.getBody();
    }

    // /**
    // * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
    // */
    // @Override
    // public void writeChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer byteBuffer)
    // {
//        // @formatter:off
//        UriComponents builder = UriComponentsBuilder.fromPath("/writeChunkBuffer")
//                .queryParam("baseDir", baseDir)
//                .queryParam("relativeFile", relativeFile)
//                .queryParam("position", position)
//                .queryParam("sizeOfChunk", sizeOfChunk)
//                .build();
//        // @formatter:on
    //
    // byteBuffer.flip();
    //
    // // DataBuffer buffer = this.dataBufferFactory.wrap(byteBuffer);
    // // buffer.readPosition(0);
    //
//        // @formatter:off
//        RestTemplate rt = this.restTemplateBuilder
//            .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE), new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
//            .build()
//            ;
//        // @formatter:on
    //
    // // Resource resource = new InputStreamResource(new ByteBufferInputStream(buffer));
    // // ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), resource, String.class);
    // ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), byteBuffer, String.class);
    // responseEntity.getBody();
    // }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeFileHandle(java.lang.String, java.lang.String, long,
     *      de.freese.jsync.filesystem.fileHandle.FileHandle, java.util.function.LongConsumer)
     */
    @Override
    public void writeFileHandle(final String baseDir, final String relativeFile, final long sizeOfFile, final FileHandle fileHandle,
                                final LongConsumer bytesWrittenConsumer)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/writeFileHandle")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("sizeOfFile", sizeOfFile)
                .build();
         // @formatter:on

        // @formatter:off
        RestTemplate rt = this.restTemplateBuilder
            .interceptors(new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE), new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE))
            .build()
            ;
        // @formatter:on

        MonitoringReadableByteChannel monitoringReadableByteChannel = new MonitoringReadableByteChannel(fileHandle.getHandle(), bytesWrittenConsumer, true);

        Resource resource = new InputStreamResource(Channels.newInputStream(monitoringReadableByteChannel));
        ResponseEntity<String> responseEntity = rt.postForEntity(builder.toUriString(), resource, String.class);

        responseEntity.getBody();
    }
}
