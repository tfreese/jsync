package de.freese.jsync.serialisation;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;
import de.freese.jsync.serialisation.serializer.ClassSerializer;
import de.freese.jsync.serialisation.serializer.ExceptionSerializer;
import de.freese.jsync.serialisation.serializer.JSyncCommandSerializer;
import de.freese.jsync.serialisation.serializer.PathFilterSerializer;
import de.freese.jsync.serialisation.serializer.SyncItemSerializer;

public interface Serializer<R, W> {
    DataReader<R> getReader();

    DataWriter<W> getWriter();

    default <T> T read(final R input, final ClassSerializer<T> classSerializer) {
        return classSerializer.read(getReader(), input);
    }

    default boolean readBoolean(final R input) {
        return getReader().readBoolean(input);
    }

    default Exception readException(final R input) {
        return read(input, ExceptionSerializer.getInstance());
    }

    default JSyncCommand readJSyncCommand(final R input) {
        return read(input, JSyncCommandSerializer.getInstance());
    }

    default Long readLong(final R input) {
        return getReader().readLong(input);
    }

    default PathFilter readPathFilter(final R input) {
        return read(input, PathFilterSerializer.getInstance());
    }

    default String readString(final R input) {
        return getReader().readString(input);
    }

    default SyncItem readSyncItem(final R input) {
        return read(input, SyncItemSerializer.getInstance());
    }

    default void write(final W output, final JSyncCommand value) {
        write(output, value, JSyncCommandSerializer.getInstance());
    }

    default void write(final W output, final PathFilter value) {
        write(output, value, PathFilterSerializer.getInstance());
    }

    default void write(final W output, final SyncItem value) {
        write(output, value, SyncItemSerializer.getInstance());
    }

    default void write(final W output, final Exception value) {
        write(output, value, ExceptionSerializer.getInstance());
    }

    default <T> void write(final W output, final T value, final ClassSerializer<T> classSerializer) {
        classSerializer.write(getWriter(), output, value);
    }

    default void writeBoolean(final W output, final boolean value) {
        getWriter().writeBoolean(output, value);
    }

    default void writeLong(final W output, final long value) {
        getWriter().writeLong(output, value);
    }

    default void writeString(final W output, final String value) {
        getWriter().writeString(output, value);
    }
}
