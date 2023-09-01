// Created: 23.11.2018
package de.freese.jsync.client.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public class LoggerClientListener extends AbstractClientListener {
    private final Logger logger = LoggerFactory.getLogger("Client");

    @Override
    public void checksumProgress(final Options options, final SyncItem syncItem, final long bytesRead) {
        String message = checksumProgressMessage(options, syncItem, bytesRead);

        if (message == null) {
            return;
        }

        getLogger().info(message);
    }

    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred) {
        String message = copyProgressMessage(options, syncItem, bytesTransferred);

        if (message == null) {
            return;
        }

        getLogger().info(message);
    }

    @Override
    public void delete(final Options options, final SyncItem syncItem) {
        String message = deleteMessage(options, syncItem);

        getLogger().info(message);
    }

    @Override
    public void error(final String message, final Throwable th) {
        getLogger().error(message, th);
    }

    @Override
    public void update(final Options options, final SyncItem syncItem) {
        String message = updateMessage(options, syncItem);

        getLogger().info(message);
    }

    @Override
    public void validate(final Options options, final SyncItem syncItem) {
        String message = validateMessage(options, syncItem);

        getLogger().info(message);
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
