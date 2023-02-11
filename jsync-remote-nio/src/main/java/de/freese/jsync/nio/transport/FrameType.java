// Created: 20.08.2021
package de.freese.jsync.nio.transport;

/**
 * See io.rsocket.frame.FrameType
 *
 * @author Thomas Freese
 */
public enum FrameType {
    DATA(1),

    ERROR(2),

    FINISH(3);

    public static FrameType fromEncodedType(final int encodedType) {
        return switch (encodedType) {
            case 1 -> DATA;
            case 2 -> ERROR;
            case 3 -> FINISH;

            default -> throw new IllegalArgumentException("Frame Type is unknown: " + encodedType);
        };
    }

    private final int encodedType;

    FrameType(final int encodedType) {
        this.encodedType = encodedType;
    }

    public int getEncodedType() {
        return this.encodedType;
    }
}
