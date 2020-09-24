// Created: 24.09.2020
package de.freese.jsync.model.serializer.neu.objects;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.model.serializer.neu.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class OptionsSerializer implements ObjectSerializer<Options>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class OptionsSerializerHolder
    {
        /**
         *
         */
        private static final OptionsSerializer INSTANCE = new OptionsSerializer();

        /**
         * Erstellt ein neues {@link OptionsSerializerHolder} Object.
         */
        private OptionsSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link OptionsSerializer}
     */
    public static OptionsSerializer getInstance()
    {
        return OptionsSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link OptionsSerializer} Object.
     */
    private OptionsSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Options readFrom(final DataAdapter<D> adapter, final D source)
    {
        // bufferSize
        // int bufferSize = adapter.readInt(source);

        // checksum
        boolean checksum = BooleanSerializer.getInstance().readFrom(adapter, source);

        // delete
        boolean delete = BooleanSerializer.getInstance().readFrom(adapter, source);

        // dryRun
        boolean dryRun = BooleanSerializer.getInstance().readFrom(adapter, source);

        // followSymLinks
        boolean followSymLinks = BooleanSerializer.getInstance().readFrom(adapter, source);

        // @formatter:off
        Options options = new Builder()
                //.bufferSize(bufferSize)
                .checksum(checksum)
                .delete(delete)
                .dryRun(dryRun)
                .followSymLinks(followSymLinks)
                .build()
                ;
        // @formatter:on

        return options;
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Options value)
    {
        // bufferSize
        // adapter.writeInt(sink, value.getBufferSize());

        // checksum
        BooleanSerializer.getInstance().writeTo(adapter, sink, value.isChecksum());

        // delete
        BooleanSerializer.getInstance().writeTo(adapter, sink, value.isDelete());

        // dryRun
        BooleanSerializer.getInstance().writeTo(adapter, sink, value.isDryRun());

        // followSymLinks
        BooleanSerializer.getInstance().writeTo(adapter, sink, value.isFollowSymLinks());
    }
}
