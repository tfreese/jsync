// Created: 18.10.2020
package de.freese.jsync.spring.rsocket;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import de.freese.jsync.spring.rsocket.model.Constants;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
@Configuration
// @Profile("!test")
public class RSocketClientConfig
{
    /**
     * @param builder {@link org.springframework.messaging.rsocket.RSocketRequester.Builder}
     * @param host String
     * @param port int
     * @return {@link RSocketRequester}
     */
    @Bean
    public RSocketRequester getRSocketRequester(final RSocketRequester.Builder builder, @Value("${spring.rsocket.server.address}") final String host,
                                                @Value("${spring.rsocket.server.port}") final int port)
    {
        ClientTransport clientTransport = TcpClientTransport.create(host, port);
        // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

        // @formatter:off
        return builder
                .rsocketConnector(rSocketConnector -> rSocketConnector.payloadDecoder(PayloadDecoder.DEFAULT).reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))))
                .transport(clientTransport)
                ;
        // @formatter:on
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
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(Constants.MIME_FILE_NAME), String.class, Constants.FILE_NAME);
                    metadataExtractorRegistry.metadataToExtract(MimeType.valueOf(Constants.MIME_FILE_EXTENSION), String.class, Constants.FILE_EXTENSION);
                })
                .build()
                ;
        // @formatter:on
    }
}
