// Created: 22.10.2016
package de.freese.jsync.model;

/**
 * Differences between Source and Target.
 *
 * @author Thomas Freese
 */
public enum SyncStatus {
    /**
     *
     */
    DIFFERENT_CHECKSUM,
    //    /**
    //     *
    //     */
    //    DIFFERENT_GROUP,
    /**
     *
     */
    DIFFERENT_LAST_MODIFIEDTIME,
    //    /**
    //     *
    //     */
    //    DIFFERENT_PERMISSIONS,
    /**
     *
     */
    DIFFERENT_SIZE,
    //    /**
    //     *
    //     */
    //    DIFFERENT_USER,
    /**
     * Source must be copied.
     */
    ONLY_IN_SOURCE,
    /**
     * Source must be deleted.
     */
    ONLY_IN_TARGET,
    /**
     * Source and Target are identical.
     */
    SYNCHRONIZED,
    /**
     *
     */
    UNKNOWN
}
