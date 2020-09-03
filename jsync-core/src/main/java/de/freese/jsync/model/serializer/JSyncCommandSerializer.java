// Created: 28.04.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import de.freese.jsync.model.JSyncCommand;

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
    static Serializer<JSyncCommand> getInstance()
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
        String name = Serializers.readFrom(buffer, String.class);

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
        Serializers.writeTo(buffer, obj.name());
    }
}
