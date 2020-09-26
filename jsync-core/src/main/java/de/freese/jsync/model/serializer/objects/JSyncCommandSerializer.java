// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class JSyncCommandSerializer implements ObjectSerializer<JSyncCommand>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class JSyncCommandSerializerHolder
    {
        /**
         *
         */
        private static final JSyncCommandSerializer INSTANCE = new JSyncCommandSerializer();

        /**
         * Erstellt ein neues {@link JSyncCommandSerializerHolder} Object.
         */
        private JSyncCommandSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link JSyncCommandSerializer}
     */
    public static JSyncCommandSerializer getInstance()
    {
        return JSyncCommandSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link JSyncCommandSerializer} Object.
     */
    private JSyncCommandSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> JSyncCommand readFrom(final DataAdapter<D> adapter, final D source)
    {
        String name = StringSerializer.getInstance().readFrom(adapter, source);

        if ((name == null) || name.isBlank())
        {
            return null;
        }

        return JSyncCommand.valueOf(name);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final JSyncCommand value)
    {
        StringSerializer.getInstance().writeTo(adapter, sink, value.name());
    }
}
