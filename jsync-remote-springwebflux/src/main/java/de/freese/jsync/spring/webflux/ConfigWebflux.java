// Created: 18.10.2020
package de.freese.jsync.spring.webflux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.utils.buffer.DataBufferAdapter;
import de.freese.jsync.utils.buffer.DefaultPooledDataBufferFactory;

/**
 * @author Thomas Freese
 */
@Configuration
// @PropertySource("classpath:webflux.properties")
public class ConfigWebflux implements WebFluxConfigurer
{
    /**
     * Erstellt ein neues {@link ConfigWebflux} Object.
     */
    public ConfigWebflux()
    {
        super();
    }

    /**
     * @see org.springframework.web.reactive.config.WebFluxConfigurer#configureHttpMessageCodecs(org.springframework.http.codec.ServerCodecConfigurer)
     */
    @Override
    public void configureHttpMessageCodecs(final ServerCodecConfigurer configurer)
    {
        configurer.registerDefaults(true);
        configurer.defaultCodecs().maxInMemorySize(Options.DATABUFFER_SIZE);
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
     * @return {@link LocalhostReceiver}
     */
    @Bean
    @Scope("prototype")
    public LocalhostReceiver receiver()
    {
        return new LocalhostReceiver();
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
