// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.freese.jsync.Options;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.BooleanSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.ExceptionSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.GroupSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.IntegerSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.LongSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.OptionsSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.StackTraceElementSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.StringSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.SyncItemSerializer;
import de.freese.jsync.model.serializer.objectserializer.impl.UserSerializer;

/**
 * @author Thomas Freese
 *
 * @param <D> Type of Source/Sink
 */
public final class DefaultSerializer<D> implements Serializer<D>, SerializerRegistry
{
    /**
     * @param adapter {@link DataAdapter}
     *
     * @return {@link Serializer}
     */
    public static <D> Serializer<D> of(final DataAdapter<D> adapter)
    {
        return new DefaultSerializer<>(adapter);
    }

    /**
     *
     */
    private final DataAdapter<D> adapter;

    /**
     *
     */
    private final Map<Class<?>, ObjectSerializer<?>> serializerMap = new HashMap<>();

    /**
     * Erstellt ein neues {@link DefaultSerializer} Object.
     *
     * @param adapter {@link DataAdapter}
     */
    private DefaultSerializer(final DataAdapter<D> adapter)
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

        register(JSyncCommand.class, new JSyncCommandSerializer());
        register(User.class, new UserSerializer());
        register(Group.class, new GroupSerializer());
        register(SyncItem.class, new SyncItemSerializer());
        register(DefaultSyncItem.class, new SyncItemSerializer());
        register(Options.class, new OptionsSerializer());
        register(StackTraceElement.class, new StackTraceElementSerializer());
        register(Exception.class, new ExceptionSerializer());
    }

    /**
     * @see de.freese.jsync.model.serializer.SerializerRegistry#getSerializer(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectSerializer<T> getSerializer(final Class<T> type)
    {
        return (ObjectSerializer<T>) this.serializerMap.get(type);
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.lang.Object, java.lang.Class)
     */
    @Override
    public <T> T readFrom(final D source, final Class<T> type)
    {
        ObjectSerializer<T> serializer = getSerializer(type);

        T value = serializer.readFrom(this, this.adapter, source);

        return value;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#register(java.lang.Class, de.freese.jsync.model.serializer.objectserializer.ObjectSerializer)
     */
    @Override
    public <T> void register(final Class<T> type, final ObjectSerializer<? super T> serializer)
    {
        this.serializerMap.put(type, serializer);
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.lang.Object, java.lang.Object, java.lang.Class)
     */
    @Override
    public <T> void writeTo(final D sink, final T value, final Class<T> type)
    {
        ObjectSerializer<T> serializer = getSerializer(type);

        serializer.writeTo(this, this.adapter, sink, value);
    }
}
