// Created: 28.04.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;

import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
class UserSerializer implements Serializer<User>
{
    /**
     *
     */
    private static final Serializer<User> INSTANCE = new UserSerializer();

    /**
     * @return Serializer<User>
     */
    static Serializer<User> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link UserSerializer} Object.
     */
    UserSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public User readFrom(final ByteBuffer buffer)
    {
        // uid
        int uid = buffer.getInt();

        // name
        String name = StringSerializer.getInstance().readFrom(buffer);

        return new User(name, uid);
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final User obj)
    {
        // uid
        buffer.putInt(obj.getUid());

        // name
        StringSerializer.getInstance().writeTo(buffer, obj.getName());
    }
}
