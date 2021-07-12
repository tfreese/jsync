// Created: 23.11.2018

package de.freese.jsync.client.listener;

import java.util.EventListener;

import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.model.SyncItem;

/**
 * Listener für den {@link Client}.
 *
 * @author Thomas Freese
 */
public interface ClientListener extends EventListener
{
    /**
     * Wird nur aufgerufen, wenn DRY-RUN = false ist.
     *
     * @param options {@link Options}
     * @param syncItem {@link SyncItem}
     * @param bytesTransferred long
     */
    void copyProgress(Options options, final SyncItem syncItem, long bytesTransferred);

    /**
     * @param options {@link Options}
     * @param syncItem {@link SyncItem}
     */
    void delete(Options options, SyncItem syncItem);

    /**
     * @param message String
     * @param th {@link Throwable}
     */
    void error(String message, Throwable th);

    /**
     * @param options {@link Options}
     * @param syncItem {@link SyncItem}
     */
    void update(Options options, final SyncItem syncItem);

    /**
     * @param options {@link Options}
     * @param syncItem {@linkSyncItem}
     */
    void validate(Options options, final SyncItem syncItem);
}
