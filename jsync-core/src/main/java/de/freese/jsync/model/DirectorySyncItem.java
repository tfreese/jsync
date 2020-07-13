/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

/**
 * Object für die Informationen eines Verzeichnisses.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public class DirectorySyncItem extends AbstractSyncItem
{
    /**
     * Erstellt ein neues {@link DirectorySyncItem} Object.
     *
     * @param relativePath String
     */
    public DirectorySyncItem(final String relativePath)
    {
        super(relativePath);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("DirectorySyncItem [relativePath=");
        builder.append(getRelativePath());
        builder.append("]");

        return builder.toString();
    }
}
