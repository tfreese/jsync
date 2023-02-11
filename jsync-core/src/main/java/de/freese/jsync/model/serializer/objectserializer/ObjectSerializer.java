// Created: 28.04.2020
package de.freese.jsync.model.serializer.objectserializer;

import java.nio.charset.Charset;

import de.freese.jsync.Options;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * Interface zur Serialisierung eines Objektes.
 *
 * @author Thomas Freese
 */
public interface ObjectSerializer<T> {
    default Charset getCharset() {
        return Options.CHARSET;
    }

    <W, R> T readFrom(SerializerRegistry registry, DataAdapter<W, R> adapter, R source);

    <W, R> void writeTo(SerializerRegistry registry, DataAdapter<W, R> adapter, W sink, T value);
}
