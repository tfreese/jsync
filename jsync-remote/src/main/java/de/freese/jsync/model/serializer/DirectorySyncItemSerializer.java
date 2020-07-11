/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import de.freese.jsync.model.DirectorySyncItem;

/**
 * @author Thomas Freese
 */
class DirectorySyncItemSerializer extends AbstractSyncItemSerializer<DirectorySyncItem>
{
    /**
    *
    */
    private static final Serializer<DirectorySyncItem> INSTANCE = new DirectorySyncItemSerializer();

    /**
     * @return Serializer<DirectorySyncItem>
     */
    static Serializer<DirectorySyncItem> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link DirectorySyncItemSerializer} Object.
     */
    DirectorySyncItemSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.AbstractSyncItemSerializer#createSyncItem(java.lang.String)
     */
    @Override
    protected DirectorySyncItem createSyncItem(final String relativePath)
    {
        DirectorySyncItem syncItem = new DirectorySyncItem(relativePath);

        return syncItem;
    }
}
