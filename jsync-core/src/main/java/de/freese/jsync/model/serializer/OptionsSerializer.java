/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
class OptionsSerializer implements Serializer<Options>
{
    /**
     *
     */
    private static final Serializer<Options> INSTANCE = new OptionsSerializer();

    /**
     * @return Serializer<Options>
     */
    static Serializer<Options> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link OptionsSerializer} Object.
     */
    OptionsSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public Options readFrom(final ByteBuffer buffer)
    {
        // bufferSize
        int bufferSize = buffer.getInt();

        // checksum
        boolean checksum = buffer.get() == 1 ? true : false;

        // delete
        boolean delete = buffer.get() == 1 ? true : false;

        // dryRun
        boolean dryRun = buffer.get() == 1 ? true : false;

        // followSymLinks
        boolean followSymLinks = buffer.get() == 1 ? true : false;

        Options options = new Options();
        options.setBufferSize(bufferSize);
        options.setChecksum(checksum);
        options.setDelete(delete);
        options.setDryRun(dryRun);
        options.setFollowSymLinks(followSymLinks);

        return options;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final Options obj)
    {
        // bufferSize
        buffer.putInt(obj.getBufferSize());

        // checksum
        buffer.put((byte) (obj.isChecksum() ? 1 : 0));

        // delete
        buffer.put((byte) (obj.isDelete() ? 1 : 0));

        // dryRun
        buffer.put((byte) (obj.isDryRun() ? 1 : 0));

        // followSymLinks
        buffer.put((byte) (obj.isFollowSymLinks() ? 1 : 0));
    }
}
