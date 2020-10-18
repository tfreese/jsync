// Created: 15.09.2020
package de.freese.jsync.spring.rest;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * POM:<br>
 * &lt;packaging>&gt;war&lt;/packaging&gt;<<br>
 * Tomcat aus spring-boot-starter-web excludieren und explizit auf provided setzen.<br>
 * Alle anderen J2EE-Jars auf provided setzen.
 *
 * @author Thomas Freese
 */
@SpringBootApplication(exclude = ReactiveWebServerFactoryAutoConfiguration.class, scanBasePackages =
{
        "de.freese.jsync.spring.rest"
})
// @ComponentScan("de.freese.jsync.spring.rest")
public class JsyncRestApplication extends SpringBootServletInitializer
{
    /**
     * Konfiguriert die SpringApplication.
     *
     * @param builder {@link SpringApplicationBuilder}
     * @return {@link SpringApplicationBuilder}
     */
    private static SpringApplicationBuilder configureApplication(final SpringApplicationBuilder builder)
    {
        //@formatter:off
        return builder
            .sources(JsyncRestApplication.class)
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
     * Erstellt ein neues {@link JsyncRestApplication} Object.
     */
    public JsyncRestApplication()
    {
        super();
    }

    /**
     * POM:<br>
     * &lt;packaging>&gt;war&lt;/packaging&gt;<<br>
     * Tomcat aus spring-boot-starter-web excludieren und explizit auf provided setzen.<br>
     * Alle anderen J2EE-Jars auf provided setzen.
     *
     * @see org.springframework.boot.web.servlet.support.SpringBootServletInitializer#configure(org.springframework.boot.builder.SpringApplicationBuilder)
     */
    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder application)
    {
        return configureApplication(application);
    }

    /**
     *
     */
    public void restart()
    {
        ApplicationArguments args = this.context.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            this.context.close();
            // this.context = SpringApplication.run(JsyncRestApplication.class, args.getSourceArgs());
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
