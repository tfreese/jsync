// Created: 12.07.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.DefaultClient;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public class JsyncController
{
    /**
     *
     */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     *
     */
    private Client client;

    /**
     *
     */
    private SyncView syncView;

    /**
     * Erstellt ein neues {@link JsyncController} Object.
     */
    public JsyncController()
    {
        super();
    }

    /**
     * @param syncView {@link SyncView}
     */
    public void init(final SyncView syncView)
    {
        this.syncView = syncView;

        syncView.doOnCompare(button -> button.addActionListener(event -> compare()));
        syncView.doOnSyncronize(button -> button.addActionListener(event -> synchronize()));
    }

    /**
     *
     */
    public void shutdown()
    {
        getLogger().info("shutdown");

        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
        }
    }

    /**
     *
     */
    private void compare()
    {
        CompareWorker worker = new CompareWorker(this);

        worker.execute();
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

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @param options {@link Options}
     * @param senderUri {@link URI}
     * @param receiverUri {@link URI}
     */
    void createNewClient(final Options options, final URI senderUri, final URI receiverUri)
    {
        if (this.client != null)
        {
            this.client.disconnectFileSystems();
            this.client = null;
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
     * @param key String
     * @return String
     */
    String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }

    /**
     * @return {@link SyncView}
     */
    SyncView getSyncView()
    {
        return this.syncView;
    }

    /**
     * @param runnable {@link Runnable}
     */
    void runInEdt(final Runnable runnable)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
