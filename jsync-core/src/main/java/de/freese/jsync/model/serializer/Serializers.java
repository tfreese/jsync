// Created: 28.04.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import de.freese.jsync.Options;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
public final class Serializers
{

    /**
     *
     */
    private static final Serializers INSTANCE = new Serializers();

    /**
     * @param buffer {@link ByteBuffer}
     * @param clazz  Class
     *
     * @return {@link JSyncCommand}
     */
    public static <T> T readFrom(final ByteBuffer buffer, final Class<T> clazz)
    {
        Serializer<T> serializer = INSTANCE.getSerializer(clazz);

        return serializer.readFrom(buffer);
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @param obj    T
     */
    public static <T> void writeTo(final ByteBuffer buffer, final T obj)
    {
        writeTo(buffer, obj, (Class<T>) obj.getClass());
    }

    /**
     * @param <T>    Type
     * @param buffer {@link ByteBuffer}
     * @param obj    T
     * @param clazz  {@link Class}
     */
    public static <T> void writeTo(final ByteBuffer buffer, final T obj, final Class<T> clazz)
    {
        Serializer<T> serializer = INSTANCE.getSerializer(clazz);

        serializer.writeTo(buffer, obj);
    }

    /**
     *
     */
    private final Map<Class<?>, Serializer<?>> serializerMap = new HashMap<>();

    /**
     * Erstellt ein neues {@link Serializers} Object.
     */
    private Serializers()
    {
        super();

        this.serializerMap.put(Boolean.class, BooleanSerializer.getInstance());
        this.serializerMap.put(String.class, StringSerializer.getInstance());

        this.serializerMap.put(Group.class, GroupSerializer.getInstance());
        this.serializerMap.put(User.class, UserSerializer.getInstance());
        this.serializerMap.put(Options.class, OptionsSerializer.getInstance());
        this.serializerMap.put(JSyncCommand.class, JSyncCommandSerializer.getInstance());
        this.serializerMap.put(SyncItem.class, SyncItemSerializer.getInstance());
        this.serializerMap.put(DefaultSyncItem.class, SyncItemSerializer.getInstance());
        this.serializerMap.put(Exception.class, ExceptionSerializer.getInstance());
        this.serializerMap.put(StackTraceElement.class, StackTracelElementSerializer.getInstance());
    }

    /**
     * @param <T>   Entity-Type
     * @param clazz Class
     *
     * @return {@link Serializer}
     */
    private <T> Serializer<T> getSerializer(final Class<T> clazz)
    {
        return (Serializer<T>) this.serializerMap.get(clazz);
    }
}
