// Created: 15.09.2020
package de.freese.jsync.spring.webflux;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * @author Thomas Freese
 */
@SpringBootApplication(exclude =
{
        ServletWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class
}, scanBasePackages =
{
        "de.freese.jsync.spring.webflux"
})
// @ComponentScan("de.freese.jsync.spring.webflux")
@EnableWebFlux
public class JsyncWebfluxApplication // extends AbstractReactiveWebInitializer
{
    /**
     * Konfiguriert die SpringApplication.
     *
     * @param builder {@link SpringApplicationBuilder}
     *
     * @return {@link SpringApplicationBuilder}
     */
    private static SpringApplicationBuilder configureApplication(final SpringApplicationBuilder builder)
    {
        //@formatter:off
        return builder
            .sources(JsyncWebfluxApplication.class)
            .bannerMode(Banner.Mode.OFF)
            .headless(true)
            .registerShutdownHook(true);
        //@formatter:on
        // .listeners(new ApplicationPidFileWriter("spring-boot-web.pid"))
        // .web(false)
    }

    /**
     * @param args String[]
     */
    public static void main(final String[] args)
    {
        configureApplication(new SpringApplicationBuilder()).run(args);
    }

    /**
     *
     */
    private ConfigurableApplicationContext context;

    // /**
    // * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer#getConfigClasses()
    // */
    // @Override
    // protected Class<?>[] getConfigClasses()
    // {
    // return new Class[]
    // {
    // JsyncWebfluxApplication.class
    // };
    // }

    /**
     *
     */
    public void restart()
    {
        ApplicationArguments args = this.context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            this.context.close();
            // this.context = SpringApplication.run(JsyncWebfluxApplication.class, args.getSourceArgs());
            this.context = configureApplication(new SpringApplicationBuilder()).run(args.getSourceArgs());
        });

        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * @param args String[]
     */
    public void start(final String[] args)
    {
        this.context = configureApplication(new SpringApplicationBuilder()).run(args);
    }

    /**
     *
     */
    public void stop()
    {
        this.context.close();
    }
}
