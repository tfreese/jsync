/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

import java.util.Objects;

/**
 * Object für die Informationen einer Datei.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public class FileSyncItem extends AbstractSyncItem
{
    /**
    *
    */
    private String checksum = null;

    /**
    *
    */
    private long size = 0;

    /**
     * Erstellt ein neues {@link FileSyncItem} Object.
     *
     * @param path String
     */
    public FileSyncItem(final String path)
    {
        super(path);
    }

    /**
     * @return String
     */
    public String getChecksum()
    {
        return this.checksum;
    }

    /**
     * @return long
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * @param checksum String
     */
    public void setChecksum(final String checksum)
    {
        this.checksum = Objects.requireNonNull(checksum, "checksum required");
    }

    /**
     * @param size long
     */
    public void setSize(final long size)
    {
        this.size = size;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder2 = new StringBuilder();
        builder2.append("FileSyncItem [relativePath=");
        builder2.append(getRelativePath());
        builder2.append("]");

        return builder2.toString();
    }
}
