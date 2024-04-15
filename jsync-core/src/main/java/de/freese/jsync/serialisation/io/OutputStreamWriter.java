// Created: 30.09.22
package de.freese.jsync.serialisation.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * @author Thomas Freese
 */
public class OutputStreamWriter implements DataWriter<OutputStream> {
    @Override
    public void writeByte(final OutputStream output, final byte value) {
        try {
            output.write(value);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeBytes(final OutputStream output, final byte[] bytes) {
        try {
            output.write(bytes);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
