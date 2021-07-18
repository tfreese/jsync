// Created: 15.08.2020
package de.freese.jsync.swing.controller;

import java.util.List;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
class SynchronizeWorker extends AbstractWorker<Void, Void> implements ClientListener
{
    /**
     * Erstellt ein neues {@link SynchronizeWorker} Object.
     *
     * @param controller {@link JsyncController}
     */
    SynchronizeWorker(final JsyncController controller)
    {
        super(controller);

        controller.getSyncView().doOnCompare(button -> button.setEnabled(false));
        controller.getSyncView().doOnSyncronize(button -> button.setEnabled(false));

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
            getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, (int) syncItem.getSize(),
                    getMessage("jsync.files.copy") + ": " + syncItem.getRelativePath());
        }

        getSyncView().addProgressBarValue(EFileSystem.SENDER, (int) bytesTransferred);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#delete(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void delete(final Options options, final SyncItem syncItem)
    {
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.delete") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        List<SyncPair> syncList = getSyncView().getSyncList();

        getClient().syncReceiver(Flux.fromIterable(syncList), this);

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

        getSyncView().clearTable();
        getSyncView().addProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSyncronize(button -> button.setEnabled(true));
        getClient().disconnectFileSystems();
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
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.update") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validate(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validate(final Options options, final SyncItem syncItem)
    {
        getSyncView().addProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
    }
}
