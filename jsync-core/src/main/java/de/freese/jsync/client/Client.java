/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.client;

import java.util.List;
import de.freese.jsync.filesystem.destination.Target;
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.SyncPair;

/**
 * Koordiniert den Abgleich zwischen {@link Source} und {@link Target}.
 * 
 * @author Thomas Freese
 */
public interface Client
{
    /**
     * Ermittelt die Differenzen von Quelle und Ziel.
     *
     * @param source {@link Source}
     * @param sourceGeneratorListener {@link GeneratorListener}; optional.
     * @param target {@link Target}
     * @param targetGeneratorListener {@link GeneratorListener}; optional.
     * @return {@link List}
     * @throws Exception Falls was schief geht.
     */
    public List<SyncPair> createSyncList(Source source, GeneratorListener sourceGeneratorListener, Target target, GeneratorListener targetGeneratorListener)
        throws Exception;

    /**
     * Synchronisiert das Ziel-Verzeichnis mit der Quelle.
     *
     * @param source {@link Source}
     * @param target {@link Target}
     * @param syncList {@link List}
     * @throws Exception Falls was schief geht.
     */
    public void syncReceiver(Source source, Target target, List<SyncPair> syncList) throws Exception;
}
