// Created: 15.08.2020
package de.freese.jsync.swing.controller;

import java.net.URI;
import java.util.List;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
class SynchronizeWorker extends SwingWorker<Void, Void> implements ClientListener
{
    /**
    *
    */
    public static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeWorker.class);

    /**
     * @return {@link Logger}
     */
    private static Logger getLogger()
    {
        return LOGGER;
    }

    /**
     *
     */
    private final JsyncController controller;

    /**
     *
     */
    private final Options options;

    /**
     *
     */
    private final URI receiverUri;

    /**
     *
     */
    private final URI senderUri;

    /**
     * Erstellt ein neues {@link SynchronizeWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    SynchronizeWorker(final JsyncController controller)
    {
        super();

        this.controller = controller;
        this.options = controller.getSyncView().getOptions();
        this.senderUri = controller.getSyncView().getUri(EFileSystem.SENDER);
        this.receiverUri = controller.getSyncView().getUri(EFileSystem.RECEIVER);

        controller.getSyncView().doOnCompare(button -> button.setEnabled(false));
        controller.getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        controller.createNewClient(this.options, this.senderUri, this.receiverUri);

        controller.getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        controller.getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
        controller.getSyncView().setProgressBarFiles(0);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#copyProgress(de.freese.jsync.Options, de.freese.jsync.model.SyncItem, long)
     */
    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        if (bytesTransferred == 0)
        {
            getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, (int) syncItem.getSize(),
                    this.controller.getMessage("jsync.files.copy") + ": " + syncItem.getRelativePath());
        }

        getSyncView().addProgressBarValue(EFileSystem.RECEIVER, (int) bytesTransferred);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#delete(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void delete(final Options options, final SyncItem syncItem)
    {
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0,
                this.controller.getMessage("jsync.files.delete") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error(final String message, final Throwable th)
    {
        getLogger().error(message, th);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#update(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final Options options, final SyncItem syncItem)
    {
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0,
                this.controller.getMessage("jsync.files.update") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validate(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validate(final Options options, final SyncItem syncItem)
    {
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0,
                this.controller.getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
    }

    @Override
    protected Void doInBackground() throws Exception
    {
        List<SyncPair> syncList = getSyncView().getSyncList();

        getClient().syncReceiver(syncList, this);

        return null;
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        try
        {
            get();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSyncronize(button -> button.setEnabled(true));
        getClient().disconnectFileSystems();
    }

    /**
     * @return {@link Client}
     */
    Client getClient()
    {
        return this.controller.getClient();
    }

    /**
     * @return {@link SyncView}
     */
    SyncView getSyncView()
    {
        return this.controller.getSyncView();
    }
}
