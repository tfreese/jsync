// Created: 18.11.2018
package de.freese.jsync.spring.filesystem.sender;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.spring.HttpHeaderInterceptor;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * {@link Sender} für Remote-Filesysteme für Spring-REST.
 *
 * @author Thomas Freese
 */
public class RemoteSenderRestTemplate extends AbstractSender
{
    /**
     *
     */
    private PoolingHttpClientConnectionManager poolingConnectionManager;

    /**
     *
     */
    private RestTemplate restTemplate;

    /**
     * Erstellt ein neues {@link RemoteSenderRestTemplate} Object.
     */
    public RemoteSenderRestTemplate()
    {
        super();
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

        // @formatter:off
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(3000)
                .setConnectTimeout(3000)
                .setSocketTimeout(3000).build()
                ;

        HttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(this.poolingConnectionManager)
                .setUserAgent("JSync")
                .build()
                ;
        // @formatter:on

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

//        // @formatter:off
//        this.restTemplateBuilder = this.restTemplateBuilder
//                .rootUri(rootUri)
//                .errorHandler(new NoOpResponseErrorHandler())
//                .requestFactory(() -> httpRequestFactory);
//        // @formatter:on

        String rootUri = String.format("http://%s:%d/jsync/sender", uri.getHost(), uri.getPort());

        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(httpRequestFactory);
        this.restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(rootUri, this.restTemplate.getUriTemplateHandler()));
        this.restTemplate.setInterceptors(List.of(new HttpHeaderInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE),
                new HttpHeaderInterceptor("Content-Type", MediaType.APPLICATION_JSON_VALUE)));

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> responseEntity = this.restTemplate.getForEntity("/connect", String.class);
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
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        // String url = String.format("/syncItems/%s/%b/", urlEncode(baseDir), followSymLinks);
        // @formatter:off
        //UriComponents builder = UriComponentsBuilder.fromPath("/checksum")
        //        .pathSegment("{baseDir}")
        //        .pathSegment("{followSymLinks}")
        //        .build(baseDir, followSymLinks);
        // @formatter:on

        // String url = "/syncItems?baseDir={baseDir}&followSymLinks={followSymLinks}";
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/syncItems")
                .queryParam("baseDir", baseDir)
                .queryParam("followSymLinks", followSymLinks)
                .build();
        // @formatter:on

        ResponseEntity<Resource> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), Resource.class);
        Resource resource = responseEntity.getBody();

        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            InputStream inputStream = resource.getInputStream();
            buffer.clear();

            while (inputStream.available() > 0)
            {
                buffer.put((byte) inputStream.read());
            }

            buffer.flip();

            @SuppressWarnings("unused")
            int itemCount = buffer.getInt();

            while (buffer.hasRemaining())
            {
                SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
                consumerSyncItem.accept(syncItem);
            }
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile, final long size)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/channel")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("size", size)
                .build();
        // @formatter:on

        ResponseEntity<Resource> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), Resource.class);
        Resource resource = responseEntity.getBody();

        try
        {
            return Channels.newChannel(resource.getInputStream());
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        // String url = String.format("/checksum/%s/%b/", baseDir, relativeFile);
        // @formatter:off
        //UriComponents builder = UriComponentsBuilder.fromPath("/checksum")
        //        .pathSegment("{baseDir}")
        //        .pathSegment("{relativeFile}")
        //        .build(baseDir, relativeFile);
        // @formatter:on

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
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        // @formatter:off
        UriComponents builder = UriComponentsBuilder.fromPath("/chunk")
                .queryParam("baseDir", baseDir)
                .queryParam("relativeFile", relativeFile)
                .queryParam("position", position)
                .queryParam("size", size)
                .build();
        // @formatter:on

        ResponseEntity<Resource> responseEntity = this.restTemplate.getForEntity(builder.toUriString(), Resource.class);
        Resource resource = responseEntity.getBody();

        try
        {
            InputStream inputStream = resource.getInputStream();
            buffer.clear();

            while (inputStream.available() > 0)
            {
                buffer.put((byte) inputStream.read());
            }
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    // /**
    // * @param value String
    // * @return String
    // */
    // protected String urlDecode(final String value)
    // {
    // if (value == null)
    // {
    // return null;
    // }
    //
    // try
    // {
    // String encoded = URLDecoder.decode(value.trim(), "UTF-8");
    //
    // return encoded;
    // }
    // catch (UnsupportedEncodingException ueex)
    // {
    // throw new RuntimeException(ueex);
    // }
    // }
    //
    // /**
    // * @param value String
    // * @return String
    // */
    // protected String urlEncode(final String value)
    // {
    // if (value == null)
    // {
    // return null;
    // }
    //
    // try
    // {
    // String encoded = URLEncoder.encode(value.trim(), "UTF-8");
    //
    // return encoded;
    // }
    // catch (UnsupportedEncodingException ueex)
    // {
    // throw new RuntimeException(ueex);
    // }
    // }
}
