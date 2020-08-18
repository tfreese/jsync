// Created: 18.08.20
package de.freese.jsync.swing.components;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import de.freese.jsync.model.SyncPair;
import de.freese.jsync.model.SyncStatus;

/**
 * @author Thomas Freese
 */
public class SyncListTableCellRenderer extends DefaultTableCellRenderer
{
    /**
     *
     */
    private static final long serialVersionUID = -8974544290640941021L;

    /**
     * @see DefaultTableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column)
    {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        SyncListTableModel tableModel = (SyncListTableModel) table.getModel();
        SyncPair syncPair = tableModel.getObjectAt(row);
        SyncStatus syncStatus = syncPair.getStatus();

        switch (column)
        {
            case 1, 2, 4 -> setHorizontalAlignment(SwingConstants.CENTER);
            default -> setHorizontalAlignment(SwingConstants.LEFT);
        }

//        if (syncStatus == null)
//        {
//            return this;
//        }

        switch (syncStatus)
        {
            case SYNCHRONIZED -> setForeground(Color.BLACK);
            case ONLY_IN_TARGET -> setForeground(Color.RED);
            default -> setForeground(Color.ORANGE.darker());
        }

//        if (SyncStatus.SYNCHRONIZED.equals(syncStatus))
//        {
//            setForeground(Color.BLACK);
//        }
//        else if (SyncStatus.ONLY_IN_TARGET.equals(syncStatus))
//        {
//            setForeground(Color.RED);
//        }
//        else
//        {
//            setForeground(Color.ORANGE.darker());
//        }

        return this;
    }
}
