// Created: 24.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class StackTraceElementSerializer implements ClassSerializer<StackTraceElement> {
    private static final class StackTraceElementSerializerHolder {
        private static final StackTraceElementSerializer INSTANCE = new StackTraceElementSerializer();

        private StackTraceElementSerializerHolder() {
            super();
        }
    }

    public static StackTraceElementSerializer getInstance() {
        return StackTraceElementSerializerHolder.INSTANCE;
    }
    
    private StackTraceElementSerializer() {
        super();
    }

    @Override
    public <R> StackTraceElement read(final DataReader<R> reader, final R input) {
        final String clazzName = reader.readString(input);
        final String methodName = reader.readString(input);
        final String fileName = reader.readString(input);
        final int lineNumber = reader.readInteger(input);

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    @Override
    public <W> void write(final DataWriter<W> writer, final W output, final StackTraceElement value) {
        writer.writeString(output, value.getClassName());
        writer.writeString(output, value.getMethodName());
        writer.writeString(output, value.getFileName());
        writer.writeInteger(output, value.getLineNumber());
    }
}
