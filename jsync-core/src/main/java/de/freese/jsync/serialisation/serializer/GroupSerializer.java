// Created: 22.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.model.Group;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class GroupSerializer {
    public static <R> Group read(final DataReader<R> reader, final R input) {
        if (reader.readByte(input) == -1) {
            return null;
        }

        // gid
        final int gid = reader.readInteger(input);

        // name
        final String name = reader.readString(input);

        return new Group(name, gid);
    }

    public static <W> void write(final DataWriter<W> writer, final W output, final Group value) {
        if (value == null) {
            writer.writeByte(output, (byte) -1);
            return;
        }

        writer.writeByte(output, (byte) 1);

        // gid
        writer.writeInteger(output, value.getGid());

        // name
        writer.writeString(output, value.getName());
    }

    private GroupSerializer() {
        super();
    }
}
