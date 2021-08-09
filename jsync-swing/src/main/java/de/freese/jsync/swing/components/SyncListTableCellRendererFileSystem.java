// Created: 18.08.20
package de.freese.jsync.swing.components;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author Thomas Freese
 */
public class SyncListTableCellRendererFileSystem extends DefaultTableCellRenderer
{
    /**
     *
     */
    private static final long serialVersionUID = -8974544290640941021L;

    /**
     * @see DefaultTableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row,
                                                   final int column)
    {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        switch (column)
        {
            case 1 -> setHorizontalAlignment(SwingConstants.CENTER);
            default -> setHorizontalAlignment(SwingConstants.LEFT);
        }

        return this;
    }
}
