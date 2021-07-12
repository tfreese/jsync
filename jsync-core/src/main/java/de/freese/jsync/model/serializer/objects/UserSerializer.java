// Created: 22.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class UserSerializer implements ObjectSerializer<User>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class UserSerializerHolder
    {
        /**
         *
         */
        private static final UserSerializer INSTANCE = new UserSerializer();

        /**
         * Erstellt ein neues {@link UserSerializerHolder} Object.
         */
        private UserSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link UserSerializer}
     */
    public static UserSerializer getInstance()
    {
        return UserSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link UserSerializer} Object.
     */
    private UserSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> User readFrom(final DataAdapter<D> adapter, final D source)
    {
        if (adapter.readInt(source) == 0)
        {
            return null;
        }

        // uid
        int uid = adapter.readInt(source);

        // name
        String name = adapter.readString(source, getCharset());

        return new User(name, uid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final User value)
    {
        if (value == null)
        {
            adapter.writeInt(sink, 0);
            return;
        }

        adapter.writeInt(sink, 1);

        // uid
        adapter.writeInt(sink, value.getUid());

        // name
        adapter.writeString(sink, value.getName(), getCharset());
    }
}
