// Created: 22.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.model.User;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class UserSerializer implements ClassSerializer<User> {
    private static final class UserSerializerHolder {
        private static final UserSerializer INSTANCE = new UserSerializer();

        private UserSerializerHolder() {
            super();
        }
    }

    public static UserSerializer getInstance() {
        return UserSerializerHolder.INSTANCE;
    }
    
    private UserSerializer() {
        super();
    }

    @Override
    public <R> User read(final DataReader<R> reader, final R input) {
        if (reader.readByte(input) == -1) {
            return null;
        }

        // uid
        final int uid = reader.readInteger(input);

        // name
        final String name = reader.readString(input);

        return new User(name, uid);
    }

    @Override
    public <W> void write(final DataWriter<W> writer, final W output, final User value) {
        if (value == null) {
            writer.writeByte(output, (byte) -1);
            return;
        }

        writer.writeByte(output, (byte) 1);

        // uid
        writer.writeInteger(output, value.getUid());

        // name
        writer.writeString(output, value.getName());
    }
}
