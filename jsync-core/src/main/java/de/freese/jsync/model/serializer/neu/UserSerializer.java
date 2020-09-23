// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
class UserSerializer implements ObjectSerializer<User>
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
    static UserSerializer getInstance()
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
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.DataAdapter)
     */
    @Override
    public User readFrom(final DataAdapter data)
    {
        // uid
        int uid = data.readInt();

        // name
        String name = data.readString(getCharset());

        return new User(name, uid);
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.DataAdapter, java.lang.Object)
     */
    @Override
    public void writeTo(final DataAdapter data, final User obj)
    {
        // uid
        data.writeInt(obj.getUid());

        // name
        data.writeString(obj.getName(), getCharset());
    }
}
