/**
 * Created: 28.04.2020
 */

package de.freese.jsync.filesystem;

import java.net.URI;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public interface FileSystem
{
    /**
     * Stellt die Verbindung zum Dateisystem her.
     *
     * @param uri {@link URI}
     */
    public void connect(final URI uri);

    /**
     * Trennt die Verbindung zum Dateisystem.
     */
    public void disconnect();

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     * @param consumerSyncItem {@link Consumer}
     */
    public void generateSyncItems(String baseDir, boolean followSymLinks, Consumer<SyncItem> consumerSyncItem);

    /**
     * Liefert den Channel zur Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @return {@link Channel}
     * @see ReadableByteChannel
     * @see WritableByteChannel
     */
    public Channel getChannel(String baseDir, final String relativeFile);

    /**
     * Liefert die Prüfsumme einer Datei.<br>
     *
     * @param baseDir String
     * @param relativeFile String
     * @param consumerBytesRead {@link LongConsumer}; optional
     * @return String
     */
    public String getChecksum(String baseDir, String relativeFile, LongConsumer consumerBytesRead);
}
