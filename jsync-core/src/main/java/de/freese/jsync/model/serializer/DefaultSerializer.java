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
import de.freese.jsync.model.serializer.objects.BooleanSerializer;
import de.freese.jsync.model.serializer.objects.CharSequenceSerializer;
import de.freese.jsync.model.serializer.objects.ExceptionSerializer;
import de.freese.jsync.model.serializer.objects.GroupSerializer;
import de.freese.jsync.model.serializer.objects.IntegerSerializer;
import de.freese.jsync.model.serializer.objects.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.objects.LongSerializer;
import de.freese.jsync.model.serializer.objects.ObjectSerializer;
import de.freese.jsync.model.serializer.objects.OptionsSerializer;
import de.freese.jsync.model.serializer.objects.StackTraceElementSerializer;
import de.freese.jsync.model.serializer.objects.StringSerializer;
import de.freese.jsync.model.serializer.objects.SyncItemSerializer;
import de.freese.jsync.model.serializer.objects.UserSerializer;

/**
 * @author Thomas Freese
 * @param <D> Type of Source/Sink
 */
public final class DefaultSerializer<D> implements Serializer<D>
{
    /**
     * @param adapter {@link DataAdapter}
     * @return {@link Serializer}
     */
    public static <D> Serializer<D> of(final DataAdapter<D> adapter)
    {
        Serializer<D> serializer = new DefaultSerializer<>(adapter);

        return serializer;
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

        register(CharSequence.class, CharSequenceSerializer.getInstance());
        register(String.class, StringSerializer.getInstance());

        register(boolean.class, BooleanSerializer.getInstance());
        register(Boolean.class, BooleanSerializer.getInstance());

        register(int.class, IntegerSerializer.getInstance());
        register(Integer.class, IntegerSerializer.getInstance());

        register(long.class, LongSerializer.getInstance());
        register(Long.class, LongSerializer.getInstance());

        register(JSyncCommand.class, JSyncCommandSerializer.getInstance());
        register(User.class, UserSerializer.getInstance());
        register(Group.class, GroupSerializer.getInstance());
        register(SyncItem.class, SyncItemSerializer.getInstance());
        register(DefaultSyncItem.class, SyncItemSerializer.getInstance());
        register(Options.class, OptionsSerializer.getInstance());
        register(StackTraceElement.class, StackTraceElementSerializer.getInstance());
        register(Exception.class, ExceptionSerializer.getInstance());
    }

    /**
     * @param <T> Type
     * @param type Class
     * @return {@link ObjectSerializer}
     */
    @SuppressWarnings("unchecked")
    private <T> ObjectSerializer<T> getSerializer(final Class<T> type)
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

        T value = serializer.readFrom(this.adapter, source);

        return value;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#register(java.lang.Class, de.freese.jsync.model.serializer.objects.ObjectSerializer)
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

        serializer.writeTo(this.adapter, sink, value);
    }
}
