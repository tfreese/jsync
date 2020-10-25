// Created: 15.09.2020
package de.freese.jsync.spring.rest;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.spring.rest.utils.DataBufferHttpMessageConverter;
import de.freese.jsync.spring.rest.utils.buffer.DataBufferAdapter;
import de.freese.jsync.spring.rest.utils.buffer.DefaultPooledDataBufferFactory;

/**
 * @author Thomas Freese
 */
@Configuration
// @PropertySource("classpath:rest.properties")
public class ConfigRest implements WebMvcConfigurer// , WebServerFactoryCustomizer<TomcatServletWebServerFactory>
{
    // /**
    // *
    // */
    // private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRest.class);

    /**
     * Erstellt ein neues {@link ConfigRest} Object.
     */
    public ConfigRest()
    {
        super();
    }

    /**
     * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer#configureMessageConverters(java.util.List)
     */
    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> converters)
    {
        // converters.add(new ByteBufferHttpMessageConverter(Options.BUFFER_SIZE, () -> ByteBufferPool.getInstance().get()));
        // converters.add(new ByteBufferHttpMessageConverter(Options.BUFFER_SIZE, () -> ByteBuffer.allocateDirect(Options.DATABUFFER_SIZE)));
        converters.add(new DataBufferHttpMessageConverter(Options.BUFFER_SIZE, DefaultPooledDataBufferFactory.getInstance()));
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

    // /**
    // * @see org.springframework.boot.web.server.WebServerFactoryCustomizer#customize(org.springframework.boot.web.server.WebServerFactory)
    // */
    // @Override
    // public void customize(final TomcatServletWebServerFactory factory)
    // {
    // factory.addConnectorCustomizers(connector -> {
    // AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
    //
    // // protocol.setConnectionTimeout(120000);
    // getLogger().info("####################################################################################");
    // getLogger().info("#");
    // getLogger().info("# TomcatCustomizer");
    // getLogger().info("#");
    // getLogger().info("# custom maxKeepAliveRequests {}", protocol.getMaxKeepAliveRequests());
    // getLogger().info("# origin keepalive timeout: {} ms", protocol.getKeepAliveTimeout());
    // getLogger().info("# keepalive timeout: {} ms", protocol.getKeepAliveTimeout());
    // getLogger().info("# connection timeout: {} ms", protocol.getConnectionTimeout());
    // getLogger().info("# connection upload timeout: {} ms", protocol.getConnectionUploadTimeout());
    // getLogger().info("# max connections: {}", protocol.getMaxConnections());
    // getLogger().info("# session timeout: {}", protocol.getSessionTimeout());
    // getLogger().info("#");
    // getLogger().info("####################################################################################");
    // });
    // }

    // /**
    // * @return EmbeddedServletContainerFactory
    // */
    // @Bean
    // public EmbeddedServletContainerFactory getEmbeddedServletContainerFactory()
    // {
    // TomcatEmbeddedServletContainerFactory containerFactory = new TomcatEmbeddedServletContainerFactory();
    // containerFactory.addConnectorCustomizers(new TomcatConnectorCustomizer()
    // {
    // @Override
    // public void customize(final Connector connector)
    // {
    // ((AbstractProtocol) connector.getProtocolHandler()).setKeepAliveTimeout(30000);
    // }
    // });
    //
    // return containerFactory;
    // }

    // /**
    // * @return {@link Logger}
    // */
    // protected Logger getLogger()
    // {
    // return LOGGER;
    // }
}
