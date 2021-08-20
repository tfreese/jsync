// Created: 20.08.2021
package de.freese.jsync.nio.transport;

/**
 * Siehe io.rsocket.frame.FrameType
 *
 * @author Thomas Freese
 */
public enum FrameType
{
    /**
     *
     */
    DATA(1),
    /**
    *
    */
    ERROR(2),
    /**
     *
     */
    FINISH(3);

    /**
     * @param encodedType int
     *
     * @return {@link FrameType}
     */
    public static FrameType fromEncodedType(final int encodedType)
    {
        return switch (encodedType)
        {
            case 1 -> DATA;
            case 2 -> ERROR;
            case 3 -> FINISH;

            default -> throw new IllegalArgumentException("Frame Type is unknown: " + encodedType);
        };
    }

    /**
     *
     */
    private final int encodedType;

    /**
     * Erstellt ein neues {@link FrameType} Object.
     *
     * @param encodedType int
     */
    FrameType(final int encodedType)
    {
        this.encodedType = encodedType;
    }

    /**
     * @return int
     */
    public int getEncodedType()
    {
        return this.encodedType;
    }
}
