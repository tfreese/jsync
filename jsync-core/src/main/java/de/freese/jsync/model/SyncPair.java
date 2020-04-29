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
    private final SyncItem source;

    /**
     *
     */
    private SyncStatus status = null;

    /**
    *
    */
    private final SyncItem target;

    /**
     * Erstellt ein neues {@link SyncPair} Object.
     *
     * @param source {@link SyncItem}; wenn null -> nur im Receiver enthalten
     * @param target {@link SyncItem}; wenn null -> nur im Sender enthalten
     */
    public SyncPair(final SyncItem source, final SyncItem target)
    {
        super();

        this.source = source;
        this.target = target;
    }

    /**
     * @return String
     */
    public String getRelativePath()
    {
        return getSource() != null ? getSource().getRelativePath() : getTarget().getRelativePath();
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
     * Wenn null -> nur im Sender enthalten.
     *
     * @return {@link SyncItem}
     */
    public SyncItem getTarget()
    {
        return this.target;
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
     *
     * @throws IllegalStateException Falls was schief geht.
     */
    public void validateStatus() throws IllegalStateException
    {
        if ((getSource() == null) && (getTarget() != null))
        {
            // Löschen: In der Quelle nicht mehr vorhanden, aber im Ziel.
            this.status = SyncStatus.ONLY_IN_TARGET;
        }
        else if ((getSource() != null) && (getTarget() == null))
        {
            // Kopieren: In der Quelle vorhanden, aber nicht im Ziel.
            this.status = SyncStatus.ONLY_IN_SOURCE;
        }
        else if ((getSource() != null) && (getTarget() != null))
        {
            // Kopieren: Datei-Attribute unterschiedlich.
            if (getSource().getLastModifiedTime() != getTarget().getLastModifiedTime())
            {
                this.status = SyncStatus.DIFFERENT_LAST_MODIFIEDTIME;
            }
            else if (!Objects.equals(getSource().getPermissionsToString(), getTarget().getPermissionsToString()))
            {
                this.status = SyncStatus.DIFFERENT_PERMISSIONS;
            }
            else if (!Objects.equals(getSource().getUser(), getTarget().getUser()))
            {
                this.status = SyncStatus.DIFFERENT_USER;
            }
            else if (!Objects.equals(getSource().getGroup(), getTarget().getGroup()))
            {
                this.status = SyncStatus.DIFFERENT_GROUP;
            }

            if (getSource() instanceof FileSyncItem)
            {
                FileSyncItem src = (FileSyncItem) getSource();
                FileSyncItem dst = (FileSyncItem) getTarget();

                if (src.getSize() != dst.getSize())
                {
                    this.status = SyncStatus.DIFFERENT_SIZE;
                }
                else if (!Objects.equals(src.getChecksum(), dst.getChecksum()))
                {
                    this.status = SyncStatus.DIFFERENT_CHECKSUM;
                }
            }

            // Alle Prüfungen ohne Unterschied.
            if (this.status == null)
            {
                this.status = SyncStatus.SYNCHRONIZED;
            }
        }
        else
        {
            throw new IllegalStateException("unknown SyncStatus");
        }
    }
}
