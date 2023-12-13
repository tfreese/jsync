// Created: 12.07.2020
package de.freese.jsync.swing.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.swing.JSyncContext;
import de.freese.jsync.swing.util.SwingUtils;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JSyncController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Client client;
    private SyncView syncView;

    public void init(final SyncView syncView) {
        this.syncView = syncView;
        this.syncView.restoreState();

        syncView.doOnCompare(button -> button.addActionListener(event -> compare()));
        syncView.doOnSynchronize(button -> button.addActionListener(event -> synchronize()));
    }

    public void shutdown() {
        getLogger().info("shutdown");

        getSyncView().saveState();

        shutdownClient();
    }

    void createNewClient(final Options options, final URI senderUri, final URI receiverUri) {
        shutdownClient();

        if ("rsocketLocal".equals(senderUri.getScheme()) || "rsocketLocal".equals(receiverUri.getScheme())) {
            JSyncContext.startLocalRSocketServer();
        }

        this.client = new DefaultClient(options, senderUri, receiverUri);
        this.client.connectFileSystems();
    }

    Client getClient() {
        return this.client;
    }

    String getMessage(final String key) {
        return JSyncContext.getMessages().getString(key);
    }

    SyncView getSyncView() {
        return this.syncView;
    }

    void runInEdt(final Runnable runnable) {
        SwingUtils.runInEdt(runnable);
    }

    void shutdownClient() {
        if (this.client != null) {
            this.client.disconnectFileSystems();
            this.client = null;
        }
    }

    protected Logger getLogger() {
        return this.logger;
    }

    private void compare() {
        final CompareWorker worker = new CompareWorker(this);

        worker.execute();
    }

    private void synchronize() {
        // JOptionPane.showMessageDialog(JSyncSwingApplication.getInstance().getMainFrame(), "Not implemented !", "Error", JOptionPane.ERROR_MESSAGE);
        final SynchronizeWorker worker = new SynchronizeWorker(this);

        worker.execute();
    }
}
