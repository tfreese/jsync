// Created: 15.08.2020
package de.freese.jsync.swing.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.freese.jsync.Options;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

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

        getSyncView().doOnCompare(button -> button.setEnabled(false));
        getSyncView().doOnSyncronize(button -> button.setEnabled(false));

        getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");

        getSyncView().setProgressBarFilesMax(0);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#checksumProgress(de.freese.jsync.Options, de.freese.jsync.model.SyncItem, long)
     */
    @Override
    public void checksumProgress(final Options options, final SyncItem syncItem, final long bytesRead)
    {
        if (bytesRead == 0)
        {
            getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, (int) syncItem.getSize(),
                    getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
        }

        getSyncView().setProgressBarValue(EFileSystem.RECEIVER, (int) bytesRead);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#copyProgress(de.freese.jsync.Options, de.freese.jsync.model.SyncItem, long)
     */
    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        if (bytesTransferred == 0)
        {
            getSyncView().incrementProgressBarFilesValue(1);

            getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, (int) syncItem.getSize(),
                    getMessage("jsync.files.copy") + ": " + syncItem.getRelativePath());
        }

        getSyncView().setProgressBarValue(EFileSystem.SENDER, (int) bytesTransferred);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#delete(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void delete(final Options options, final SyncItem syncItem)
    {
        getSyncView().incrementProgressBarFilesValue(1);

        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.delete") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see javax.swing.SwingWorker#doInBackground()
     */
    @Override
    protected Void doInBackground() throws Exception
    {
        List<SyncPair> syncPairs = getSyncView().getSyncList();

        getSyncView().setProgressBarFilesMax(syncPairs.size());

        getClient().syncReceiver(syncPairs, this);

        // Wir warten etwas bis alle GUI-Events verarbeitet wurden.
        TimeUnit.MILLISECONDS.sleep(200);

        return null;
    }

    /**
     * @see javax.swing.SwingWorker#done()
     */
    @Override
    protected void done()
    {
        getLogger().info("{}", ByteBufferPool.DEFAULT);

        try
        {
            get();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }

        getSyncView().clearTable();
        getSyncView().setProgressBarMinMaxText(EFileSystem.SENDER, 0, 0, "");
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, "");
        getSyncView().setProgressBarFilesMax(0);
        getSyncView().doOnCompare(button -> button.setEnabled(true));
        getSyncView().doOnSyncronize(button -> button.setEnabled(true));
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
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.update") + ": " + syncItem.getRelativePath());
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validate(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validate(final Options options, final SyncItem syncItem)
    {
        getSyncView().setProgressBarMinMaxText(EFileSystem.RECEIVER, 0, 0, getMessage("jsync.files.validate") + ": " + syncItem.getRelativePath());
    }
}
