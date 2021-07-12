// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

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
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Options readFrom(final DataAdapter<D> adapter, final D source)
    {
        // bufferSize
        // int bufferSize = adapter.readInt(source);

        // checksum
        boolean checksum = adapter.readBoolean(source);

        // delete
        boolean delete = adapter.readBoolean(source);

        // dryRun
        boolean dryRun = adapter.readBoolean(source);

        // followSymLinks
        boolean followSymLinks = adapter.readBoolean(source);

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
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Options value)
    {
        // bufferSize
        // adapter.writeInt(sink, value.getBufferSize());

        // checksum
        adapter.writeBoolean(sink, value.isChecksum());

        // delete
        adapter.writeBoolean(sink, value.isDelete());

        // dryRun
        adapter.writeBoolean(sink, value.isDryRun());

        // followSymLinks
        adapter.writeBoolean(sink, value.isFollowSymLinks());
    }
}
