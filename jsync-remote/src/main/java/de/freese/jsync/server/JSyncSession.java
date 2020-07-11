/**
 * Created: 14.11.2018
 */

package de.freese.jsync.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.Sender;
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
    private Receiver receiver = null;

    /**
    *
    */
    private Sender sender = null;

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
     * @return {@link Receiver}
     */
    public Receiver getReceiver()
    {
        return this.receiver;
    }

    /**
     * @return {@link Sender}
     */
    public Sender getSender()
    {
        return this.sender;
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
     * @param receiver {@link Receiver}
     */
    public void setReceiver(final Receiver receiver)
    {
        this.receiver = receiver;
    }

    /**
     * @param sender {@link Sender}
     */
    public void setSender(final Sender sender)
    {
        this.sender = sender;
    }
}
