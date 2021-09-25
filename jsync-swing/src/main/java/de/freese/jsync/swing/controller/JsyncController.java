// Created: 12.07.2020
package de.freese.jsync.swing.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.swing.JsyncContext;
import de.freese.jsync.swing.util.SwingUtils;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     *
     */
    private Client client;
    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     *
     */
    private SyncView syncView;

    /**
     *
     */
    private void compare()
    {
        CompareWorker worker = new CompareWorker(this);

        worker.execute();
    }

    /**
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     */
    void createNewClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        shutdownClient();

        if ("rsocketLocal".equals(senderUri.getScheme()) || "rsocketLocal".equals(receiverUri.getScheme()))
        {
            JsyncContext.startLocalRsocketServer();
        }

        this.client = new DefaultClient(options, senderUri, receiverUri);
        this.client.connectFileSystems();
    }

    /**
     * @return {@link Client}
     */
    Client getClient()
    {
        return this.client;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param key String
     *
     * @return String
     */
    String getMessage(final String key)
    {
        return JsyncContext.getMessages().getString(key);
    }

    /**
     * @return {@link SyncView}
     */
    SyncView getSyncView()
    {
        return this.syncView;
    }

    /**
     * @param syncView {@link SyncView}
     */
    public void init(final SyncView syncView)
    {
        this.syncView = syncView;
        this.syncView.restoreState();

        syncView.doOnCompare(button -> button.addActionListener(event -> compare()));
        syncView.doOnSyncronize(button -> button.addActionListener(event -> synchronize()));
    }

    /**
     * @param runnable {@link Runnable}
     */
    void runInEdt(final Runnable runnable)
    {
        SwingUtils.runInEdt(runnable);
    }

    /**
     *
     */
    public void shutdown()
    {
        getLogger().info("shutdown");

        getSyncView().saveState();

        shutdownClient();
    }

    /**
     *
     */
    void shutdownClient()
    {
        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
        }
    }

    /**
     *
     */
    private void synchronize()
    {
        // JOptionPane.showMessageDialog(JSyncSwingApplication.getInstance().getMainFrame(), "Not implemented !", "Error", JOptionPane.ERROR_MESSAGE);
        SynchronizeWorker worker = new SynchronizeWorker(this);

        worker.execute();
    }
}
