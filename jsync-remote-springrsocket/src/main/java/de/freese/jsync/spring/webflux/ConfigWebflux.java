// Created: 18.10.2020
package de.freese.jsync.spring.webflux;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan("de.freese.jsync.spring.webflux")
@PropertySource("classpath:webflux.properties")
@EnableWebFlux
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
        configurer.defaultCodecs().maxInMemorySize(Options.DATABUFFER_SIZE);
    }
}
