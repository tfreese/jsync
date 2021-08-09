/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.components;

import java.util.List;
import java.util.Objects;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JsyncContext;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class SyncPairTableModel extends AbstractListTableModel<SyncPair>
{
    /**
     *
     */
    private static final long serialVersionUID = -5500863230405594620L;

    /**
     *
     */
    private final EFileSystem fileSystem;

    /**
     * Erstellt ein neues {@link SyncPairTableModel} Object.
     *
     * @param fileSystem {@link EFileSystem}
     */
    public SyncPairTableModel(final EFileSystem fileSystem)
    {
        super(List.of(JsyncContext.getMessages().getString("jsync.name"), JsyncContext.getMessages().getString("jsync.size")));

        this.fileSystem = Objects.requireNonNull(fileSystem, "fileSystem required");
    }

    /**
     * @see de.freese.jsync.swing.components.AbstractListTableModel#getColumnClass(int)
     */
    @Override
    public Class<? extends Object> getColumnClass(final int columnIndex)
    {
        return switch (columnIndex)
        {
            case 0, 1 -> String.class;
            default -> Object.class;
        };
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncPair syncPair = getObjectAt(rowIndex);
        SyncItem syncItem = EFileSystem.SENDER.equals(this.fileSystem) ? syncPair.getSenderItem() : syncPair.getReceiverItem();

        return switch (columnIndex)
        {
            case 0 -> syncItem != null ? syncItem.getRelativePath() : null;
            case 1 -> (syncItem != null) && syncItem.isFile() ? JSyncUtils.toHumanReadableSize(syncItem.getSize()) : null;
            default -> null;
        };
    }
}
