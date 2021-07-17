// Created: 22.10.2016
package de.freese.jsync.generator;

import java.nio.file.Path;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Der Generator sammelt alle relevanten Informationens des Dateisystems für den gewählten {@link Path}.
 *
 * @author Thomas Freese
 */
public interface Generator
{
    /**
     * Erzeugt die Prüfsumme einer Datei.<br>
     *
     * @param baseDir String
     * @param relativeFile String
     * @param consumerBytesRead {@link LongConsumer}; optional
     *
     * @return {@link Mono}
     */
    Mono<String> generateChecksum(final String baseDir, String relativeFile, final LongConsumer consumerBytesRead);

    /**
     * Erzeugt die SyncItems (Verzeichnisse, Dateien) des Basis-Verzeichnisses.<br>
     *
     * @param baseDir String
     * @param followSymLinks boolean
     *
     * @return {@link Flux}
     */
    Flux<SyncItem> generateItems(final String baseDir, boolean followSymLinks);
}
