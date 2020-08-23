// Created: 14.11.2018
package de.freese.jsync.server;

import java.nio.ByteBuffer;
import java.util.Objects;

import de.freese.jsync.Options;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import org.slf4j.Logger;

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
    private final Logger logger;

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
     * @param checksum String
     */
    public void setChecksum(final String checksum)
    {
        this.checksum = checksum;
    }

    /**
     * @return {@link JSyncCommand}
     */
    public JSyncCommand getLastCommand()
    {
        return this.lastCommand;
    }

    /**
     * @param lastCommand {@link JSyncCommand}
     */
    public void setLastCommand(final JSyncCommand lastCommand)
    {
        this.lastCommand = lastCommand;
    }

    /**
     * @return {@link Logger}
     */
    public Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link SyncItem}
     */
    public SyncItem getSyncItem()
    {
        return this.syncItem;
    }

    /**
     * @param syncItem {@link SyncItem}
     */
    public void setSyncItem(final SyncItem syncItem)
    {
        this.syncItem = syncItem;
    }

    /**
     * @return boolean
     */
    public boolean isFollowSymLinks()
    {
        return this.followSymLinks;
    }

    /**
     * @param followSymLinks boolean
     */
    public void setFollowSymLinks(final boolean followSymLinks)
    {
        this.followSymLinks = followSymLinks;
    }
}
