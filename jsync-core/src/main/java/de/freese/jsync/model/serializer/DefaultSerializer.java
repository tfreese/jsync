// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.freese.jsync.Options;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.model.serializer.adapter.impl.InputOutputStreamAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.BooleanSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.DoubleSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.ExceptionSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.FloatSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.GroupSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.IntegerSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.LongSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.OptionsSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.PathFilterSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.StackTraceElementSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.StringSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.SyncItemSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.UserSerializer;

/**
 * @param <W> Type of Sink
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public final class DefaultSerializer<W, R> implements Serializer<W, R>, SerializerRegistry
{
    public static <W, R> Serializer<W, R> of(final DataAdapter<W, R> adapter)
    {
        return new DefaultSerializer<>(adapter);
    }

    public static Serializer<ByteBuffer, ByteBuffer> ofByteBuffer()
    {
        return of(new ByteBufferAdapter());
    }

    public static Serializer<OutputStream, InputStream> ofInputOutputStream()
    {
        return of(new InputOutputStreamAdapter());
    }

    private final DataAdapter<W, R> adapter;

    private final Map<Class<?>, ObjectSerializer<?>> serializerMap = new HashMap<>();

    private DefaultSerializer(final DataAdapter<W, R> adapter)
    {
        super();

        this.adapter = Objects.requireNonNull(adapter, "adapter required");

        register(String.class, new StringSerializer());

        register(boolean.class, new BooleanSerializer());
        register(Boolean.class, getSerializer(boolean.class));

        register(int.class, new IntegerSerializer());
        register(Integer.class, getSerializer(int.class));

        register(long.class, new LongSerializer());
        register(Long.class, getSerializer(long.class));

        register(float.class, new FloatSerializer());
        register(Float.class, getSerializer(float.class));

        register(double.class, new DoubleSerializer());
        register(Double.class, getSerializer(double.class));

        register(JSyncCommand.class, new JSyncCommandSerializer());
        register(User.class, new UserSerializer());
        register(Group.class, new GroupSerializer());
        register(SyncItem.class, new SyncItemSerializer());
        register(DefaultSyncItem.class, new SyncItemSerializer());
        register(Options.class, new OptionsSerializer());
        register(StackTraceElement.class, new StackTraceElementSerializer());
        register(Exception.class, new ExceptionSerializer());
        register(PathFilter.class, new PathFilterSerializer());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectSerializer<T> getSerializer(final Class<T> type)
    {
        ObjectSerializer<T> serializer = (ObjectSerializer<T>) this.serializerMap.get(type);

        if (serializer == null)
        {
            for (Class<?> ifc : type.getInterfaces())
            {
                serializer = (ObjectSerializer<T>) this.serializerMap.get(ifc);

                if (serializer != null)
                {
                    break;
                }
            }
        }

        return serializer;
    }

    @Override
    public <T> T readFrom(final R source, final Class<T> type)
    {
        ObjectSerializer<T> serializer = getSerializer(type);

        return serializer.readFrom(this, this.adapter, source);
    }

    @Override
    public <T> void register(final Class<T> type, final ObjectSerializer<? super T> serializer)
    {
        this.serializerMap.put(type, serializer);
    }

    @Override
    public <T> void writeTo(final W sink, final T value, final Class<T> type)
    {
        ObjectSerializer<T> serializer = getSerializer(type);

        serializer.writeTo(this, this.adapter, sink, value);
    }
}
