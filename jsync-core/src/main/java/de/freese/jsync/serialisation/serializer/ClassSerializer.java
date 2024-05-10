package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

public interface ClassSerializer<T> {
    <R> T read(DataReader<R> reader, R input);

    <W> void write(DataWriter<W> writer, W output, T value);
}
