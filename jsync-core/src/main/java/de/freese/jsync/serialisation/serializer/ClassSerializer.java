package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

public interface ClassSerializer<T> {
    <R> T read(final DataReader<R> reader, final R input);

    <W> void write(final DataWriter<W> writer, final W output, final T value);
}
