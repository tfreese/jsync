// Created: 18.10.2020
package de.freese.jsync.spring.rsocket;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.spring.rsocket.model.Constants;
import de.freese.jsync.utils.buffer.DataBufferAdapter;
import de.freese.jsync.utils.buffer.DefaultPooledDataBufferFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
@Configuration
// @Profile("!test")
public class ConfigRSocket
{
    /**
     * Erstellt ein neues {@link ConfigRSocket} Object.
     */
    public ConfigRSocket()
    {
        super();
    }

    /**
     * @return {@link DataBufferFactory}
     */
    @Bean
    public DataBufferFactory dataBufferFactory()
    {
        DataBufferFactory dataBufferFactory = DefaultPooledDataBufferFactory.getInstance();

        return dataBufferFactory;
    }

    /**
     * @param builder {@link Builder}
     * @return {@link Mono}
     */
    @Bean
    public Mono<RSocketRequester> getRSocketRequester(final RSocketRequester.Builder builder)
    {
        // @formatter:off
        return builder
                .rsocketConnector(rSocketConnector -> rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))))
                .connect(TcpClientTransport.create(6565));
        // @formatter:on
    }

    /**
     * @return {@link LocalhostReceiver}
     */
    @Bean
    @Scope("prototype")
    public LocalhostReceiver receiver()
    {
        return new LocalhostReceiver();
    }

    /**
     * @return {@link RSocketStrategies}
     */
    @Bean
    public RSocketStrategies rSocketStrategies()
    {
        // @formatter:off
        return RSocketStrategies.builder()
                .encoders(encoders -> encoders.add(new Jackson2CborEncoder()))
                .decoders(decoders -> decoders.add(new Jackson2CborDecoder()))
                .metadataExtractorRegistry(metadataExtractorRegistry -> {
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(Constants.MIME_FILE_EXTENSION), String.class, Constants.FILE_EXTENSION);
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(Constants.MIME_FILE_NAME), String.class, Constants.FILE_NAME);
                }).build();
        // @formatter:on
    }

    /**
     * @return {@link LocalhostSender}
     */
    @Bean
    @Scope("prototype")
    public LocalhostSender sender()
    {
        return new LocalhostSender();
    }

    /**
     * @return {@link Serializer}
     */
    @Bean
    public Serializer<DataBuffer> serializer()
    {
        Serializer<DataBuffer> serializer = DefaultSerializer.of(new DataBufferAdapter());

        return serializer;
    }
}
