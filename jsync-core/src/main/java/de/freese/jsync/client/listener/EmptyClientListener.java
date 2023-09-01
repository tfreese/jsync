// Created: 26.07.2020
package de.freese.jsync.client.listener;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class EmptyClientListener implements ClientListener {
    @Override
    public void checksumProgress(final Options options, final SyncItem syncItem, final long bytesRead) {
        // Empty
    }

    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred) {
        // Empty
    }

    @Override
    public void delete(final Options options, final SyncItem syncItem) {
        // Empty
    }

    @Override
    public void error(final String message, final Throwable th) {
        // Empty
    }

    @Override
    public void update(final Options options, final SyncItem syncItem) {
        // Empty
    }

    @Override
    public void validate(final Options options, final SyncItem syncItem) {
        // Empty
    }
}
