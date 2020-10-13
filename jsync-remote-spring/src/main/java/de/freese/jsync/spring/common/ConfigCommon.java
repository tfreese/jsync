// Created: 13.10.2020
package de.freese.jsync.spring.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.utils.buffer.DataBufferAdapter;
import de.freese.jsync.utils.buffer.DefaultPooledDataBufferFactory;

/**
 * @author Thomas Freese
 */
@Configuration
@ComponentScan("de.freese.jsync.spring.common")
public class ConfigCommon
{
    /**
     * Erstellt ein neues {@link ConfigCommon} Object.
     */
    public ConfigCommon()
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
     * @return {@link Serializer}
     */
    @Bean
    public Serializer<DataBuffer> serializer()
    {
        Serializer<DataBuffer> serializer = DefaultSerializer.of(new DataBufferAdapter());

        return serializer;
    }
}
