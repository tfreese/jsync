package de.freese.jsync.serialisation;

import java.util.Objects;

import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

public final class DefaultSerializer<R, W> implements Serializer<R, W> {
    private final DataReader<R> dataReader;
    private final DataWriter<W> dataWriter;

    public DefaultSerializer(final DataReader<R> dataReader, final DataWriter<W> dataWriter) {
        super();

        this.dataReader = Objects.requireNonNull(dataReader, "dataReader required");
        this.dataWriter = Objects.requireNonNull(dataWriter, "dataWriter required");
    }

    @Override
    public DataReader<R> getReader() {
        return dataReader;
    }

    @Override
    public DataWriter<W> getWriter() {
        return dataWriter;
    }
}
