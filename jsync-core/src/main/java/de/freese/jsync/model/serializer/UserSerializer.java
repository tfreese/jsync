/**
 * Created: 28.04.2020
 */

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
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String name = new String(bytes, getCharset());

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
        byte[] bytes = obj.getName().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
}
