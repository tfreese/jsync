// Created: 05.04.2018
package de.freese.jsync.filesystem;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.utils.JSyncUtils;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractFileSystem implements FileSystem
{
    /**
    *
    */
    private final Path basePath;

    /**
     *
     */
    private final URI baseUri;

    /**
    *
    */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erzeugt eine neue Instanz von {@link AbstractFileSystem}.
     *
     * @param baseUri {@link URI}
     */
    public AbstractFileSystem(final URI baseUri)
    {
        super();

        this.baseUri = Objects.requireNonNull(baseUri, "baseUri required");
        this.basePath = Paths.get(JSyncUtils.normalizedPath(baseUri));
    }

    /**
     * @return {@link Path}
     */
    protected Path getBasePath()
    {
        return this.basePath;
    }

    /**
     * @return {@link URI}
     */
    protected URI getBaseUri()
    {
        return this.baseUri;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }
}
