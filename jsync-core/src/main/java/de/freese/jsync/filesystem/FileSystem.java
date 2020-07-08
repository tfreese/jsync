/**
 * Created: 28.04.2020
 */

package de.freese.jsync.filesystem;

import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public interface FileSystem
{
    /**
     * Stellt die Verbindung zum Dateisystem her.
     *
     * @throws Exception Falls was schief geht.
     */
    public void connect() throws Exception;

    /**
     * Erzeugt die Map aller SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link Map}
     */
    public Map<String, SyncItem> createSyncItems(GeneratorListener listener);

    /**
     * Trennt die Verbindung zum Dateisystem .
     *
     * @throws Exception Falls was schief geht.
     */
    public void disconnect() throws Exception;

    /**
     * Liefert den Channel zur Datei.
     *
     * @param syncItem {@link FileSyncItem}
     * @return {@link Channel}
     * @throws Exception Falls was schief geht.
     * @see ReadableByteChannel
     * @see WritableByteChannel
     */
    public Channel getChannel(final FileSyncItem syncItem) throws Exception;
}
