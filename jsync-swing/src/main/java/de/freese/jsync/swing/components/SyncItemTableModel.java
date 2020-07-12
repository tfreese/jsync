/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.components;

import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
public class SyncItemTableModel extends AbstractListTableModel<SyncItem>
{
    /**
     *
     */
    private static final long serialVersionUID = -2044934758499148574L;

    /**
     * Erstellt ein neues {@link SyncItemTableModel} Object.
     */
    public SyncItemTableModel()
    {
        super(2);
    }

    /**
     * @param syncItem {@link SyncItem}
     */
    public void add(final SyncItem syncItem)
    {
        getList().add(syncItem);

        fireTableRowsInserted(getList().size(), getList().size());
    }

    /**
     *
     */
    public void clear()
    {
        getList().clear();

        refresh();
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
     * @see de.freese.jsync.swing.components.AbstractListTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(final int column)
    {
        switch (column)
        {
            case 0:
            case 1:
            default:
                return super.getColumnName(column);
        }
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex)
    {
        SyncItem syncItem = getObjectAt(rowIndex);
        Object value = null;

        switch (columnIndex)
        {
            case 0:
                value = syncItem.getRelativePath();
                break;
            case 1:
                if (syncItem instanceof FileSyncItem)
                {
                    value = JSyncUtils.toHumanReadableSize(((FileSyncItem) syncItem).getSize());
                }
                break;

            default:
                break;
        }

        return value;
    }
}
