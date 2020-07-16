/**
 * Created: 28.04.2020
 */

package de.freese.jsync.filesystem;

import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncItemMeta;

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
     * Liefert die Meta-Daten des {@link SyncItem}.<br>
     *
     * @param relativePath String
     * @param followSymLinks boolean
     * @param withChecksum boolean
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link SyncItemMeta}
     */
    public SyncItemMeta getSyncItemMeta(String relativePath, boolean followSymLinks, boolean withChecksum, GeneratorListener listener);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param followSymLinks boolean
     * @param listener {@link GeneratorListener}; optional.
     * @return {@link List}
     */
    public List<SyncItem> getSyncItems(boolean followSymLinks, GeneratorListener listener);
}
