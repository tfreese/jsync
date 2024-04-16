// Created: 15.08.2021
package de.freese.jsync.serialisation.serializer;

import java.util.HashSet;
import java.util.Set;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class PathFilterSerializer implements ClassSerializer<PathFilter> {
    private static final class PathFilterSerializerHolder {
        private static final PathFilterSerializer INSTANCE = new PathFilterSerializer();

        private PathFilterSerializerHolder() {
            super();
        }
    }

    public static PathFilterSerializer getInstance() {
        return PathFilterSerializerHolder.INSTANCE;
    }
    
    private PathFilterSerializer() {
        super();
    }

    @Override
    public <R> PathFilter read(final DataReader<R> reader, final R input) {
        int count = reader.readInteger(input);

        final Set<String> directoryFilters = new HashSet<>();

        for (int i = 0; i < count; i++) {
            directoryFilters.add(reader.readString(input));
        }

        count = reader.readInteger(input);

        final Set<String> fileFilters = new HashSet<>();

        for (int i = 0; i < count; i++) {
            fileFilters.add(reader.readString(input));
        }

        if (directoryFilters.isEmpty() && fileFilters.isEmpty()) {
            return PathFilterNoOp.INSTANCE;
        }

        return new PathFilterEndsWith(directoryFilters, fileFilters);
    }

    @Override
    public <W> void write(final DataWriter<W> writer, final W output, final PathFilter value) {
        Set<String> filters = value.getDirectoryFilter();
        writer.writeInteger(output, filters.size());
        filters.forEach(filter -> writer.writeString(output, filter));

        filters = value.getFileFilter();
        writer.writeInteger(output, filters.size());
        filters.forEach(filter -> writer.writeString(output, filter));
    }
}
