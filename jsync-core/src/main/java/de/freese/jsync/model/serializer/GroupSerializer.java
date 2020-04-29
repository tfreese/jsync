/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import de.freese.jsync.model.Group;

/**
 * @author Thomas Freese
 */
class GroupSerializer implements Serializer<Group>
{
    /**
     *
     */
    private static final Serializer<Group> INSTANCE = new GroupSerializer();

    /**
     * @return Serializer<Group>
     */
    static Serializer<Group> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link GroupSerializer} Object.
     */
    GroupSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public Group readFrom(final ByteBuffer buffer)
    {
        // gid
        int gid = buffer.getInt();

        // name
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String name = new String(bytes, getCharset());

        return new Group(name, gid);
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final Group obj)
    {
        // gid
        buffer.putInt(obj.getGid());

        // name
        byte[] bytes = obj.getName().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
}
