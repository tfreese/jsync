// Created: 15.09.2020
package de.freese.jsync.spring.rsocket;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Thomas Freese
 */
@SpringBootApplication(scanBasePackages =
{
        "de.freese.jsync.spring.rsocket"
})
public class JsyncRSocketApplication
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
            .sources(JsyncRSocketApplication.class)
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

    /**
     *
     */
    public void restart()
    {
        ApplicationArguments args = this.context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            this.context.close();
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
