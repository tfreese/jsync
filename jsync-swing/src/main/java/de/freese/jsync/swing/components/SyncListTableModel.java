/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.components;

import java.util.List;
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
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.groesse"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.status"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.name"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.groesse")));
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
            // default -> {
            // System.err.println("Exception";
            // yield "..."; // return Value
            // }
        };
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncPair syncPair = getObjectAt(rowIndex);

        Object value = switch (columnIndex)
        {
            case 0 -> syncPair.getSenderItem() != null ? syncPair.getSenderItem().getRelativePath() : null;
            case 1 -> (syncPair.getSenderItem() != null) && syncPair.getSenderItem().isFile()
                    ? JSyncUtils.toHumanReadableSize(syncPair.getSenderItem().getSize()) : null;
            case 2 -> syncPair.getStatus();
            case 3 -> syncPair.getReceiverItem() != null ? syncPair.getReceiverItem().getRelativePath() : null;
            case 4 -> (syncPair.getReceiverItem() != null) && syncPair.getReceiverItem().isFile()
                    ? JSyncUtils.toHumanReadableSize(syncPair.getReceiverItem().getSize()) : null;
            default -> null;
            // default -> {
            // System.err.println("Exception";
            // yield "..."; // return Value
            // }
        };

        return value;
    }
}
