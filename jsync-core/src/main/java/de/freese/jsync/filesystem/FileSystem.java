/**
 * Created: 28.04.2020
 */

package de.freese.jsync.filesystem;

import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
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
     * @throws Exception Falls was schief geht.
     */
    public void connect() throws Exception;

    /**
     * Trennt die Verbindung zum Dateisystem .
     *
     * @throws Exception Falls was schief geht.
     */
    public void disconnect() throws Exception;

    /**
     * Liefert den Channel zur Datei.
     *
     * @param syncItem {@link SyncItem}
     * @return {@link Channel}
     * @throws Exception Falls was schief geht.
     * @see ReadableByteChannel
     * @see WritableByteChannel
     */
    public Channel getChannel(final SyncItem syncItem) throws Exception;

    /**
     * Liefert die Prüfsumme einer Datei.<br>
     *
     * @param relativePath String
     * @param consumerBytesRead {@link LongConsumer}; optional
     * @return String
     * @throws Exception Falls was schief geht.
     */
    public String getChecksum(String relativePath, LongConsumer consumerBytesRead) throws Exception;

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses alphabetisch sortiert.<br>
     *
     * @param followSymLinks boolean
     * @return {@link List}
     */
    public List<SyncItem> getSyncItems(boolean followSymLinks);
}
