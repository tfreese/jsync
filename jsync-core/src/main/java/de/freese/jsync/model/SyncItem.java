/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

/**
 * Interface für ein Verzeichnis oder Datei.<br>
 *
 * @author Thomas Freese
 */
public interface SyncItem
{
    /**
     * Meta-Daten, GROUP, USER, Size etc...
     *
     * @return {@link SyncItemMeta}
     */
    public SyncItemMeta getMeta();

    /**
     * Verzeichnis/Datei relativ zum Basis-Verzeichnis.
     *
     * @return String
     */
    public String getRelativePath();

    /**
     * @return boolean
     */
    public default boolean isDirectory()
    {
        return !isFile();
    }

    /**
     * @return boolean
     */
    public default boolean isFile()
    {
        return getMeta().isFile();
    }

    /**
     * Meta-Daten, GROUP, USER, Size etc...
     *
     * @param meta {@link SyncItemMeta}
     */
    public void setMeta(SyncItemMeta meta);
}
