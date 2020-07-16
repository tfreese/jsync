/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

import java.util.Objects;

/**
 * Basis-Implementierung für ein Verzeichnis / Datei, welche es zu Synchronisieren gilt.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public class DefaultSyncItem implements SyncItem
{
    /**
     *
     */
    private SyncItemMeta meta = null;

    /**
    *
    */
    private final String relativePath;

    /**
     * Erstellt ein neues {@link DefaultSyncItem} Object.
     *
     * @param relativePath String
     */
    public DefaultSyncItem(final String relativePath)
    {
        super();

        this.relativePath = Objects.requireNonNull(relativePath, "relativePath required");
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getMeta()
     */
    @Override
    public SyncItemMeta getMeta()
    {
        return this.meta;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getRelativePath()
     */
    @Override
    public String getRelativePath()
    {
        return this.relativePath;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setMeta(de.freese.jsync.model.SyncItemMeta)
     */
    @Override
    public void setMeta(final SyncItemMeta meta)
    {
        this.meta = meta;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        // sb.append(getClass().getSimpleName()).append(" [");
        sb.append("SyncItem [");
        sb.append("relativePath=").append(getRelativePath());
        sb.append("]");

        return sb.toString();
    }
}
