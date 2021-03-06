// Created: 22.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * Object für die Informationen der Source- und Destination.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public class SyncPair
{
    /**
     *
     */
    private final SyncItem receiverItem;

    /**
     *
     */
    private final SyncItem senderItem;

    /**
     *
     */
    private SyncStatus status;

    /**
     * Erstellt ein neues {@link SyncPair} Object.
     *
     * @param senderItem {@link SyncItem}; wenn null -> nur im Receiver enthalten
     * @param receiverItem {@link SyncItem}; wenn null -> nur im Sender enthalten
     */
    public SyncPair(final SyncItem senderItem, final SyncItem receiverItem)
    {
        super();

        this.senderItem = senderItem;
        this.receiverItem = receiverItem;
    }

    /**
     * Wenn null -> nur im Sender enthalten.
     *
     * @return {@link SyncItem}
     */
    public SyncItem getReceiverItem()
    {
        return this.receiverItem;
    }

    /**
     * @return String
     */
    public String getRelativePath()
    {
        return getSenderItem() != null ? getSenderItem().getRelativePath() : getReceiverItem().getRelativePath();
    }

    /**
     * Wenn null -> nur im Receiver enthalten.
     *
     * @return {@link SyncItem}
     */
    public SyncItem getSenderItem()
    {
        return this.senderItem;
    }

    /**
     * Liefert den Status der Datei.
     *
     * @return {@link SyncStatus}
     */
    public SyncStatus getStatus()
    {
        return this.status;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SyncPair [");
        builder.append("relativePath=").append(getRelativePath());
        builder.append(", status=").append(getStatus());
        builder.append("]");

        return builder.toString();
    }

    /**
     * Vergleicht die Datei in der Quelle (Source) mit dem Ziel (Target).
     */
    public void validateStatus()
    {
        if ((getSenderItem() == null) && (getReceiverItem() != null))
        {
            // Löschen: In der Quelle nicht vorhanden, aber im Ziel.
            this.status = SyncStatus.ONLY_IN_TARGET;
        }
        else if ((getSenderItem() != null) && (getReceiverItem() == null))
        {
            // Kopieren: In der Quelle vorhanden, aber nicht im Ziel.
            this.status = SyncStatus.ONLY_IN_SOURCE;
        }
        else if ((getSenderItem() != null) && (getReceiverItem() != null))
        {
            // Kopieren: Datei-Attribute unterschiedlich.
            if (getSenderItem().getLastModifiedTime() != getReceiverItem().getLastModifiedTime())
            {
                this.status = SyncStatus.DIFFERENT_LAST_MODIFIEDTIME;
            }
            else if (!Objects.equals(getSenderItem().getPermissionsToString(), getReceiverItem().getPermissionsToString()))
            {
                this.status = SyncStatus.DIFFERENT_PERMISSIONS;
            }
            else if (!Objects.equals(getSenderItem().getUser(), getReceiverItem().getUser()))
            {
                this.status = SyncStatus.DIFFERENT_USER;
            }
            else if (!Objects.equals(getSenderItem().getGroup(), getReceiverItem().getGroup()))
            {
                this.status = SyncStatus.DIFFERENT_GROUP;
            }
            else if (getSenderItem().getSize() != getReceiverItem().getSize())
            {
                this.status = SyncStatus.DIFFERENT_SIZE;
            }
            else if (!Objects.equals(getSenderItem().getChecksum(), getReceiverItem().getChecksum()))
            {
                this.status = SyncStatus.DIFFERENT_CHECKSUM;
            }
            else
            {
                // Alle Prüfungen ohne Unterschied.
                this.status = SyncStatus.SYNCHRONIZED;
            }
        }
        else
        {
            throw new IllegalStateException("unknown SyncStatus");
        }
    }
}
