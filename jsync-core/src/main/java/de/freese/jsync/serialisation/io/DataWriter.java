// Created: 22.09.2020
package de.freese.jsync.serialisation.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @param <W> Type of Output
 *
 * @author Thomas Freese
 */
public interface DataWriter<W> {
    default void writeBoolean(final W output, final boolean value) {
        writeByte(output, (byte) (value ? 1 : 0));
    }

    default void writeBooleanOrNull(final W output, final Boolean value) {
        if (value == null) {
            writeByte(output, (byte) -1);
        }
        else {
            writeByte(output, (byte) 1);
            writeBoolean(output, value);
        }
    }

    void writeByte(W output, byte value);

    void writeBytes(W output, byte[] bytes);

    default void writeDouble(final W output, final double value) {
        final long longValue = Double.doubleToRawLongBits(value);

        writeLong(output, longValue);
    }

    default void writeDoubleOrNull(final W output, final Double value) {
        if (value == null) {
            writeByte(output, (byte) -1);
        }
        else {
            writeByte(output, (byte) 1);
            writeDouble(output, value);
        }
    }

    default void writeFloat(final W output, final float value) {
        final int intValue = Float.floatToRawIntBits(value);

        writeInteger(output, intValue);
    }

    default void writeFloatOrNull(final W output, final Float value) {
        if (value == null) {
            writeByte(output, (byte) -1);
        }
        else {
            writeByte(output, (byte) 1);
            writeFloat(output, value);
        }
    }

    default void writeInteger(final W output, final int value) {
        final byte[] bytes = new byte[4];

        bytes[0] = (byte) (0xFF & (value >> 24));
        bytes[1] = (byte) (0xFF & (value >> 16));
        bytes[2] = (byte) (0xFF & (value >> 8));
        bytes[3] = (byte) (0xFF & value);

        writeBytes(output, bytes);
    }

    default void writeIntegerOrNull(final W output, final Integer value) {
        if (value == null) {
            writeByte(output, (byte) -1);
        }
        else {
            writeByte(output, (byte) 1);
            writeInteger(output, value);
        }
    }

    default void writeLong(final W output, final long value) {
        final byte[] bytes = new byte[8];

        bytes[0] = (byte) (0xFF & (value >> 56));
        bytes[1] = (byte) (0xFF & (value >> 48));
        bytes[2] = (byte) (0xFF & (value >> 40));
        bytes[3] = (byte) (0xFF & (value >> 32));
        bytes[4] = (byte) (0xFF & (value >> 24));
        bytes[5] = (byte) (0xFF & (value >> 16));
        bytes[6] = (byte) (0xFF & (value >> 8));
        bytes[7] = (byte) (0xFF & value);

        writeBytes(output, bytes);
    }

    default void writeLongOrNull(final W output, final Long value) {
        if (value == null) {
            writeByte(output, (byte) -1);
        }
        else {
            writeByte(output, (byte) 1);
            writeLong(output, value);
        }
    }

    default void writeString(final W output, final String value) {
        writeString(output, value, StandardCharsets.UTF_8);
    }

    default void writeString(final W output, final String value, final Charset charset) {
        if (value == null) {
            writeInteger(output, -1);
            return;
        }
        else if (value.isBlank()) {
            writeInteger(output, 0);
            return;
        }

        final byte[] bytes = value.getBytes(charset);

        writeInteger(output, bytes.length);
        writeBytes(output, bytes);
    }
}
