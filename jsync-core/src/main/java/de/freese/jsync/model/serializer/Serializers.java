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
    private static final byte[] EOL = new byte[]
    {
            0x45, 0x4F, 0x4C
    };

    /**
     *
     */
    private static final Serializers INSTANCE = new Serializers();

    /**
     * @return int
     */
    public static int getLengthOfEOL()
    {
        return EOL.length;
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @return boolean
     */
    public static boolean isEOL(final ByteBuffer buffer)
    {
        if ((buffer.limit() - buffer.position()) < 3)
        {
            // Buffer hat keine 3 Bytes mehr.
            return false;
        }

        int index = buffer.position();

        byte e = buffer.get(index);
        byte o = buffer.get(index + 1);
        byte l = buffer.get(index + 2);

        return (EOL[0] == e) && (EOL[1] == o) && (EOL[2] == l);
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @param clazz Class
     * @return {@link JSyncCommand}
     */
    public static <T> T readFrom(final ByteBuffer buffer, final Class<T> clazz)
    {
        Serializer<T> serializer = INSTANCE.getSerializer(clazz);

        return serializer.readFrom(buffer);
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    public static void writeEOL(final ByteBuffer buffer)
    {
        buffer.put(EOL);
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @param obj T
     */
    @SuppressWarnings("unchecked")
    public static <T> void writeTo(final ByteBuffer buffer, final T obj)
    {
        Serializer<T> serializer = INSTANCE.getSerializer((Class<T>) obj.getClass());

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
        this.serializerMap.put(Long.class, LongSerializer.getInstance());
        this.serializerMap.put(String.class, StringSerializer.getInstance());

        this.serializerMap.put(Group.class, GroupSerializer.getInstance());
        this.serializerMap.put(User.class, UserSerializer.getInstance());
        this.serializerMap.put(Options.class, OptionsSerializer.getInstance());
        this.serializerMap.put(JSyncCommand.class, JSyncCommandSerializer.getInstance());
        this.serializerMap.put(SyncItem.class, SyncItemSerializer.getInstance());
        this.serializerMap.put(DefaultSyncItem.class, SyncItemSerializer.getInstance());
    }

    /**
     * @param <T> Entity-Type
     * @param clazz Class
     * @return {@link Serializer}
     */
    @SuppressWarnings("unchecked")
    private <T> Serializer<T> getSerializer(final Class<T> clazz)
    {
        return (Serializer<T>) this.serializerMap.get(clazz);
    }
}
