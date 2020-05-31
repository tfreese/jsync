/**
 * Created: 14.11.2018
 */

package de.freese.jsync.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.destination.Target;
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.model.FileSyncItem;

/**
 * Session-Object für den Server.
 *
 * @author Thomas Freese
 */
public class JSyncSession
{
    /**
    *
    */
    private final ByteBuffer buffer;

    /**
    *
    */
    private FileSyncItem fileSyncItem = null;

    /**
     *
     */
    private JSyncCommand lastCommand = null;

    /**
     *
     */
    private final Logger logger;

    /**
     *
     */
    private final Options options;

    /**
    *
    */
    private Source source = null;

    /**
     *
     */
    private Target target = null;

    /**
     * Erstellt ein neues {@link JSyncSession} Object.
     *
     * @param options {@link Options}
     * @param logger {@link Logger}
     */
    public JSyncSession(final Options options, final Logger logger)
    {
        super();

        this.options = Objects.requireNonNull(options, "options required");
        this.buffer = ByteBuffer.allocateDirect(options.getBufferSize());
        this.logger = Objects.requireNonNull(logger, "logger required");
    }

    /**
     * @return {@link ByteBuffer}
     */
    public ByteBuffer getBuffer()
    {
        return this.buffer;
    }

    /**
     * @return {@link FileSyncItem}
     */
    public FileSyncItem getFileSyncItem()
    {
        return this.fileSyncItem;
    }

    /**
     * @return {@link JSyncCommand}
     */
    public JSyncCommand getLastCommand()
    {
        return this.lastCommand;
    }

    /**
     * @return {@link Logger}
     */
    public Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link Options}
     */
    public Options getOptions()
    {
        return this.options;
    }

    /**
     * @return {@link Source}
     */
    public Source getSource()
    {
        return this.source;
    }

    /**
     * @return {@link Target}
     */
    public Target getTarget()
    {
        return this.target;
    }

    /**
     * @param fileSyncItem {@link FileSyncItem}
     */
    public void setFileSyncItem(final FileSyncItem fileSyncItem)
    {
        this.fileSyncItem = fileSyncItem;
    }

    /**
     * @param lastCommand {@link JSyncCommand}
     */
    public void setLastCommand(final JSyncCommand lastCommand)
    {
        this.lastCommand = lastCommand;
    }

    /**
     * @param source {@link Source}
     */
    public void setSource(final Source source)
    {
        this.source = source;
    }

    /**
     * @param target {@link Target}
     */
    public void setTarget(final Target target)
    {
        this.target = target;
    }
}
