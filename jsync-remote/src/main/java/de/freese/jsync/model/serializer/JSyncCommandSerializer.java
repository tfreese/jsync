/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import de.freese.jsync.server.JSyncCommand;

/**
 * @author Thomas Freese
 */
public class JSyncCommandSerializer implements Serializer<JSyncCommand>
{
    /**
     *
     */
    private static final Serializer<JSyncCommand> INSTANCE = new JSyncCommandSerializer();

    /**
     * @return Serializer<JSyncCommand>
     */
    public static Serializer<JSyncCommand> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link JSyncCommandSerializer} Object.
     */
    JSyncCommandSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public JSyncCommand readFrom(final ByteBuffer buffer)
    {
        // byte code = buffer.get();
        //
        // JSyncCommand command = JSyncCommand.getByCode(code);

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String name = new String(bytes, getCharset());

        if (!name.isBlank())
        {
            return JSyncCommand.valueOf(name);
        }

        return null;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final JSyncCommand obj)
    {
        // buffer.put(obj.getCode());
        byte[] bytes = obj.name().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
}
