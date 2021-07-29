/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.components;

import java.util.List;

import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.JSyncSwingApplication;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class SyncListTableModel extends AbstractListTableModel<SyncPair>
{
    /**
     *
     */
    private static final long serialVersionUID = -2044934758499148574L;

    /**
     * Erstellt ein neues {@link SyncListTableModel} Object.
     */
    public SyncListTableModel()
    {
        super(List.of(JSyncSwingApplication.getInstance().getMessages().getString("jsync.name"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.size"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.status"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.name"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.size")));
    }

    /**
     * @see de.freese.jsync.swing.components.AbstractListTableModel#getColumnClass(int)
     */
    @Override
    public Class<? extends Object> getColumnClass(final int columnIndex)
    {
        return switch (columnIndex)
        {
            case 0, 1, 2, 3, 4 -> String.class;
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
        SyncItem senderItem = syncPair.getSenderItem();
        SyncItem receiverItem = syncPair.getReceiverItem();

        return switch (columnIndex)
        {
            case 0 -> senderItem != null ? senderItem.getRelativePath() : null;
            case 1 -> (senderItem != null) && senderItem.isFile() ? JSyncUtils.toHumanReadableSize(senderItem.getSize()) : null;
            case 2 -> syncPair.getStatus();
            case 3 -> receiverItem != null ? receiverItem.getRelativePath() : null;
            case 4 -> (receiverItem != null) && receiverItem.isFile() ? JSyncUtils.toHumanReadableSize(receiverItem.getSize()) : null;
            default -> null;
        };
    }
}
