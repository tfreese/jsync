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
public class SenderTableModel extends AbstractListTableModel<SyncPair>
{
    /**
     *
     */
    private static final long serialVersionUID = -2044934758499148574L;

    /**
     * Erstellt ein neues {@link SenderTableModel} Object.
     */
    public SenderTableModel()
    {
        super(List.of(JSyncSwingApplication.getInstance().getMessages().getString("jsync.name"),
                JSyncSwingApplication.getInstance().getMessages().getString("jsync.grösse")));
    }

    /**
     * @see de.freese.jsync.swing.components.AbstractListTableModel#getColumnClass(int)
     */
    @Override
    public Class<? extends Object> getColumnClass(final int columnIndex)
    {
        switch (columnIndex)
        {
            case 0:
            case 1:
                return String.class;

            default:
                return Object.class;
        }
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncPair syncPair = getObjectAt(rowIndex);
        SyncItem syncItem = syncPair.getSenderItem();

        if (syncItem == null)
        {
            return null;
        }

        Object value = null;

        switch (columnIndex)
        {
            case 0:
                value = syncItem.getRelativePath();
                break;
            case 1:
                if (syncItem.isFile())
                {
                    value = JSyncUtils.toHumanReadableSize(syncItem.getSize());
                }
                break;

            default:
                break;
        }

        return value;
    }
}
