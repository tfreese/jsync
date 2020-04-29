/**
 * Created: 14.11.2018
 */

package de.freese.jsync.server;

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
    CONNECT((byte) 0),

    /**
    *
    */
    SOURCE_CREATE_SYNC_ITEMS((byte) 1),

    /**
    *
    */
    SOURCE_READABLE_FILE_CHANNEL((byte) 2),

    /**
    *
    */
    TARGET_CREATE_DIRECTORY((byte) 3),

    /**
    *
    */
    TARGET_CREATE_SYNC_ITEMS((byte) 4),

    /**
    *
    */
    TARGET_DELETE_DIRECTORY((byte) 5),

    /**
    *
    */
    TARGET_DELETE_FILE((byte) 6),

    /**
    *
    */
    TARGET_UPDATE_DIRECTORY((byte) 7),

    /**
    *
    */
    TARGET_UPDATE_FILE((byte) 8),

    /**
    *
    */
    TARGET_VALIDATE_FILE((byte) 9),

    /**
    *
    */
    TARGET_WRITEABLE_FILE_CHANNEL((byte) 10);

    /**
     * @param code int
     * @return {@link JSyncCommand}
     */
    public static JSyncCommand getByCode(final int code)
    {
        for (JSyncCommand cmd : values())
        {
            if (cmd.getCode() == code)
            {
                return cmd;
            }
        }

        return null;
    }

    /**
     *
     */
    private final byte code;

    /**
     * Erstellt ein neues {@link JSyncCommand} Object.
     *
     * @param code byte
     */
    private JSyncCommand(final byte code)
    {
        this.code = code;
    }

    /**
     * @return byte
     */
    public byte getCode()
    {
        return this.code;
    }
}
