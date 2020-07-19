/**
 * Created: 22.10.2016
 */
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
    private final SyncItem receiver;

    /**
     *
     */
    private final SyncItem source;

    /**
     *
     */
    private SyncStatus status = null;

    /**
     * Erstellt ein neues {@link SyncPair} Object.
     *
     * @param source {@link SyncItem}; wenn null -> nur im Receiver enthalten
     * @param receiver {@link SyncItem}; wenn null -> nur im Sender enthalten
     */
    public SyncPair(final SyncItem source, final SyncItem receiver)
    {
        super();

        this.source = source;
        this.receiver = receiver;
    }

    /**
     * Wenn null -> nur im Sender enthalten.
     *
     * @return {@link SyncItem}
     */
    public SyncItem getReceiver()
    {
        return this.receiver;
    }

    /**
     * @return String
     */
    public String getRelativePath()
    {
        return getSource() != null ? getSource().getRelativePath() : getReceiver().getRelativePath();
    }

    /**
     * Wenn null -> nur im Receiver enthalten.
     *
     * @return {@link SyncItem}
     */
    public SyncItem getSource()
    {
        return this.source;
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
        builder.append("SyncPair [relativePath=");
        builder.append(getRelativePath());
        builder.append(", status=");
        builder.append(getStatus());
        builder.append("]");

        return builder.toString();
    }

    /**
     * Vergleicht die Datei in der Quelle (Source) mit dem Ziel (Target).
     */
    public void validateStatus()
    {
        if ((getSource() == null) && (getReceiver() != null))
        {
            // Löschen: In der Quelle nicht vorhanden, aber im Ziel.
            this.status = SyncStatus.ONLY_IN_TARGET;
        }
        else if ((getSource() != null) && (getReceiver() == null))
        {
            // Kopieren: In der Quelle vorhanden, aber nicht im Ziel.
            this.status = SyncStatus.ONLY_IN_SOURCE;
        }
        else if ((getSource() != null) && (getReceiver() != null))
        {
            // Kopieren: Datei-Attribute unterschiedlich.
            if (getSource().getLastModifiedTime() != getReceiver().getLastModifiedTime())
            {
                this.status = SyncStatus.DIFFERENT_LAST_MODIFIEDTIME;
            }
            else if (!Objects.equals(getSource().getPermissionsToString(), getReceiver().getPermissionsToString()))
            {
                this.status = SyncStatus.DIFFERENT_PERMISSIONS;
            }
            else if (!Objects.equals(getSource().getUser(), getReceiver().getUser()))
            {
                this.status = SyncStatus.DIFFERENT_USER;
            }
            else if (!Objects.equals(getSource().getGroup(), getReceiver().getGroup()))
            {
                this.status = SyncStatus.DIFFERENT_GROUP;
            }
            else if (getSource().getSize() != getReceiver().getSize())
            {
                this.status = SyncStatus.DIFFERENT_SIZE;
            }
            else if (!Objects.equals(getSource().getChecksum(), getReceiver().getChecksum()))
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
