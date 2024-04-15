package de.freese.jsync.serialisation;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;
import de.freese.jsync.serialisation.serializer.ExceptionSerializer;
import de.freese.jsync.serialisation.serializer.JSyncCommandSerializer;
import de.freese.jsync.serialisation.serializer.PathFilterSerializer;
import de.freese.jsync.serialisation.serializer.SyncItemSerializer;

public interface Serializer<R, W> {
    DataReader<R> getReader();

    DataWriter<W> getWriter();

    default boolean readBoolean(final R input) {
        return getReader().readBoolean(input);
    }

    default Exception readException(final R input) {
        return ExceptionSerializer.read(getReader(), input);
    }

    default JSyncCommand readJSyncCommand(final R input) {
        return JSyncCommandSerializer.read(getReader(), input);
    }

    default Long readLong(final R input) {
        return getReader().readLong(input);
    }

    default PathFilter readPathFilter(final R input) {
        return PathFilterSerializer.read(getReader(), input);
    }

    default String readString(final R input) {
        return getReader().readString(input);
    }

    default SyncItem readSyncItem(final R input) {
        return SyncItemSerializer.read(getReader(), input);
    }

    default void write(final W output, final JSyncCommand value) {
        JSyncCommandSerializer.write(getWriter(), output, value);
    }

    default void write(final W output, final PathFilter value) {
        PathFilterSerializer.write(getWriter(), output, value);
    }

    default void write(final W output, final SyncItem value) {
        SyncItemSerializer.write(getWriter(), output, value);
    }

    default void write(final W output, final Exception value) {
        ExceptionSerializer.write(getWriter(), output, value);
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
