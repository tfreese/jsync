/**
 * Created: 14.11.2018
 */

package de.freese.jsync.model;

/**
 * Remote-Commands für den Server.
 *
 * @author Thomas Freese
 */
public enum JSyncCommand
{
    /**
    *
    */
    CONNECT,

    /**
    *
    */
    DISCONNECT,

    /**
    *
    */
    SOURCE_CHECKSUM,

    /**
    *
    */
    SOURCE_CREATE_SYNC_ITEMS,

    /**
    *
    */
    SOURCE_READABLE_FILE_CHANNEL,

    /**
    *
    */
    TARGET_CHECKSUM,

    /**
    *
    */
    TARGET_CREATE_SYNC_ITEMS,

    /**
    *
    */
    TARGET_DELETE_DIRECTORY,

    /**
    *
    */
    TARGET_DELETE_FILE,

    /**
    *
    */
    TARGET_UPDATE_DIRECTORY,

    /**
    *
    */
    TARGET_UPDATE_FILE,

    /**
    *
    */
    TARGET_VALIDATE_FILE,

    /**
    *
    */
    TARGET_WRITEABLE_FILE_CHANNEL;
}
