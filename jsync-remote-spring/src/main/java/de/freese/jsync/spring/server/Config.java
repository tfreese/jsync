// Created: 15.09.2020
package de.freese.jsync.spring.server;

import java.nio.ByteBuffer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.spring.utils.ByteBufferHttpMessageConverter;
import de.freese.jsync.spring.utils.DataBufferHttpMessageConverter;
import de.freese.jsync.utils.buffer.DefaultPooledDataBufferFactory;

/**
 * @author Thomas Freese
 */
@Configuration
public class Config implements WebMvcConfigurer// , WebServerFactoryCustomizer<TomcatServletWebServerFactory>
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    /**
     * Erstellt ein neues {@link Config} Object.
     */
    public Config()
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
        converters.add(new ByteBufferHttpMessageConverter(Options.BUFFER_SIZE, () -> ByteBuffer.allocateDirect(Options.BYTEBUFFER_SIZE)));
        converters.add(new DataBufferHttpMessageConverter(Options.BUFFER_SIZE, DefaultPooledDataBufferFactory.getInstance()));
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

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
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
}
