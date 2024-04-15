// Created: 22.09.2020
package de.freese.jsync.serialisation.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @param <R> Type of Input
 *
 * @author Thomas Freese
 */
public interface DataReader<R> {
    default boolean readBoolean(final R input) {
        return readByte(input) == 1;
    }

    @SuppressWarnings("java:S2447")
    default Boolean readBooleanOrNull(final R input) {
        if (readByte(input) == -1) {
            return null;
        }

        return readBoolean(input);
    }

    byte readByte(R input);

    byte[] readBytes(R input, int length);

    default double readDouble(final R input) {
        final long longValue = readLong(input);

        return Double.longBitsToDouble(longValue);
    }

    default Double readDoubleOrNull(final R input) {
        if (readByte(input) == -1) {
            return null;
        }

        return readDouble(input);
    }

    default float readFloat(final R input) {
        final int intValue = readInteger(input);

        return Float.intBitsToFloat(intValue);
    }

    default Float readFloatOrNull(final R input) {
        if (readByte(input) == -1) {
            return null;
        }

        return readFloat(input);
    }

    default int readInteger(final R input) {
        final byte[] bytes = readBytes(input, 4);

        return ((bytes[0] & 0xFF) << 24)
                + ((bytes[1] & 0xFF) << 16)
                + ((bytes[2] & 0xFF) << 8)
                + (bytes[3] & 0xFF);
    }

    default Integer readIntegerOrNull(final R input) {
        if (readByte(input) == -1) {
            return null;
        }

        return readInteger(input);
    }

    default long readLong(final R input) {
        final byte[] bytes = readBytes(input, 8);

        return ((long) (bytes[0] & 0xFF) << 56)
                + ((long) (bytes[1] & 0xFF) << 48)
                + ((long) (bytes[2] & 0xFF) << 40)
                + ((long) (bytes[3] & 0xFF) << 32)
                + ((long) (bytes[4] & 0xFF) << 24)
                + ((long) (bytes[5] & 0xFF) << 16)
                + ((long) (bytes[6] & 0xFF) << 8)
                + ((long) bytes[7] & 0xFF);
    }

    default Long readLongOrNull(final R input) {
        if (readByte(input) == -1) {
            return null;
        }

        return readLong(input);
    }

    default String readString(final R input) {
        return readString(input, StandardCharsets.UTF_8);
    }

    default String readString(final R input, final Charset charset) {
        final int length = readInteger(input);

        if (length == -1) {
            return null;
        }
        else if (length == 0) {
            return "";
        }

        final byte[] bytes = readBytes(input, length);

        return new String(bytes, charset);
    }
}
