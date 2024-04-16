// Created: 24.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class JSyncCommandSerializer implements ClassSerializer<JSyncCommand> {
    private static final class JSyncCommandSerializerHolder {
        private static final JSyncCommandSerializer INSTANCE = new JSyncCommandSerializer();

        private JSyncCommandSerializerHolder() {
            super();
        }
    }

    public static JSyncCommandSerializer getInstance() {
        return JSyncCommandSerializerHolder.INSTANCE;
    }
    
    private JSyncCommandSerializer() {
        super();
    }

    @Override
    public <R> JSyncCommand read(final DataReader<R> dataReader, final R input) {
        final String name = dataReader.readString(input);

        if (name == null || name.isBlank()) {
            return null;
        }

        return JSyncCommand.valueOf(name);
    }

    @Override
    public <W> void write(final DataWriter<W> dataWriter, final W output, final JSyncCommand value) {
        dataWriter.writeString(output, value.name());
    }
}
