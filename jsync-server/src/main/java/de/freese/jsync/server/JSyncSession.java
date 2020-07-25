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
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;

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
    private String checksum = null;

    /**
    *
    */
    private boolean followSymLinks = true;

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
    private Receiver receiver = null;

    /**
    *
    */
    private Sender sender = null;

    /**
    *
    */
    private SyncItem syncItem = null;

    /**
     * Erstellt ein neues {@link JSyncSession} Object.
     *
     * @param logger {@link Logger}
     */
    public JSyncSession(final Logger logger)
    {
        super();

        this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
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
     * @return String
     */
    public String getChecksum()
    {
        return this.checksum;
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
     * @return {@link SyncItem}
     */
    public SyncItem getSyncItem()
    {
        return this.syncItem;
    }

    /**
     * @return boolean
     */
    public boolean isFollowSymLinks()
    {
        return this.followSymLinks;
    }

    /**
     * @param checksum String
     */
    public void setChecksum(final String checksum)
    {
        this.checksum = checksum;
    }

    /**
     * @param followSymLinks boolean
     */
    public void setFollowSymLinks(final boolean followSymLinks)
    {
        this.followSymLinks = followSymLinks;
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

    /**
     * @param syncItem {@link SyncItem}
     */
    public void setSyncItem(final SyncItem syncItem)
    {
        this.syncItem = syncItem;
    }
}
