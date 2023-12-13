// Created: 22.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * Object for Informationen about Source- and Target-Destination.<br>
 *
 * @author Thomas Freese
 */
public class SyncPair {
    private final SyncItem receiverItem;
    private final SyncItem senderItem;

    private SyncStatus status = SyncStatus.UNKNOWN;

    /**
     * Only one of the {@link SyncItem}s can be null.
     *
     * @param senderItem {@link SyncItem}; if null only existing in Receiver
     * @param receiverItem {@link SyncItem}; if null only existing in Sender
     */
    public SyncPair(final SyncItem senderItem, final SyncItem receiverItem) {
        super();

        this.senderItem = senderItem;
        this.receiverItem = receiverItem;

        if ((senderItem == null) && (receiverItem == null)) {
            throw new IllegalArgumentException("only one SyncItem can be null");
        }
    }

    public SyncItem getReceiverItem() {
        return this.receiverItem;
    }

    public String getRelativePath() {
        return getSenderItem() != null ? getSenderItem().getRelativePath() : getReceiverItem().getRelativePath();
    }

    public SyncItem getSenderItem() {
        return this.senderItem;
    }

    public SyncStatus getStatus() {
        return this.status;
    }

    public boolean isFile() {
        return getSenderItem() != null ? getSenderItem().isFile() : getReceiverItem().isFile();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SyncPair [");
        builder.append("relativePath=").append(getRelativePath());
        builder.append(", status=").append(getStatus());
        builder.append("]");

        return builder.toString();
    }

    /**
     * Compares Source with Target.
     */
    public void validateStatus() {
        if ((getSenderItem() == null) && (getReceiverItem() != null)) {
            // Delete: only available in Target but not in Source.
            this.status = SyncStatus.ONLY_IN_TARGET;
        }
        else if ((getSenderItem() != null) && (getReceiverItem() == null)) {
            // Copy: only available in Source but not in Target.
            this.status = SyncStatus.ONLY_IN_SOURCE;
        }
        else if ((getSenderItem() != null) && (getReceiverItem() != null)) {
            // Copy: Different Attributes
            if (getSenderItem().getLastModifiedTime() != getReceiverItem().getLastModifiedTime()) {
                this.status = SyncStatus.DIFFERENT_LAST_MODIFIEDTIME;
            }
            else if (getSenderItem().getSize() != getReceiverItem().getSize()) {
                this.status = SyncStatus.DIFFERENT_SIZE;
            }
            else if (!Objects.equals(getSenderItem().getChecksum(), getReceiverItem().getChecksum())) {
                this.status = SyncStatus.DIFFERENT_CHECKSUM;
            }
            //            else if (!Objects.equals(getSenderItem().getPermissionsToString(), getReceiverItem().getPermissionsToString()))
            //            {
            //                this.status = SyncStatus.DIFFERENT_PERMISSIONS;
            //            }
            //            else if (!Objects.equals(getSenderItem().getUser(), getReceiverItem().getUser()))
            //            {
            //                this.status = SyncStatus.DIFFERENT_USER;
            //            }
            //            else if (!Objects.equals(getSenderItem().getGroup(), getReceiverItem().getGroup()))
            //            {
            //                this.status = SyncStatus.DIFFERENT_GROUP;
            //            }
            else {
                this.status = SyncStatus.SYNCHRONIZED;
            }
        }
        else {
            this.status = SyncStatus.UNKNOWN;
        }
    }
}
