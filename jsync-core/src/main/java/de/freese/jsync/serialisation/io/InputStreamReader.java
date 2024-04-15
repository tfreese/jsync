// Created: 30.09.22
package de.freese.jsync.serialisation.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author Thomas Freese
 */
public class InputStreamReader implements DataReader<InputStream> {
    @Override
    public byte readByte(final InputStream input) {
        try {
            return (byte) input.read();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public byte[] readBytes(final InputStream input, final int length) {
        try {
            return input.readNBytes(length);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
