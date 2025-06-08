// Created: 14.08.2021
package de.freese.jsync.swing.view;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import de.freese.jsync.filesystem.EFileSystem;
import de.freese.jsync.model.SyncPair;
import de.freese.jsync.swing.components.AbstractListTableModel;
import de.freese.jsync.swing.components.SyncPairTableModelFileSystem;
import de.freese.jsync.swing.components.SyncPairTableModelStatus;

/**
 * @author Thomas Freese
 */
public class TableFacade {
    private final AbstractListTableModel<SyncPair> tableModelReceiver;
    private final AbstractListTableModel<SyncPair> tableModelSender;
    private final AbstractListTableModel<SyncPair> tableModelStatus;
    private final JTable tableReceiver;
    private final JTable tableSender;
    private final JTable tableStatus;

    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterReceiver;
    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterSender;
    private TableRowSorter<AbstractListTableModel<SyncPair>> rowSorterStatus;

    public TableFacade() {
        super();

        tableSender = new JTable();
        tableStatus = new JTable();
        tableReceiver = new JTable();

        tableModelSender = new SyncPairTableModelFileSystem(EFileSystem.SENDER);
        tableModelStatus = new SyncPairTableModelStatus();
        tableModelReceiver = new SyncPairTableModelFileSystem(EFileSystem.RECEIVER);
    }

    void addAll(final List<SyncPair> objects) {
        tableModelSender.addAll(objects);
        tableModelStatus.addAll(objects);
        tableModelReceiver.addAll(objects);
    }

    void clear() {
        tableModelSender.clear();
        tableModelStatus.clear();
        tableModelReceiver.clear();
    }

    void fireTableRowsUpdated(final int firstRow, final int lastRow) {
        tableModelSender.fireTableRowsUpdated(firstRow, lastRow);
        tableModelStatus.fireTableRowsUpdated(firstRow, lastRow);
        tableModelReceiver.fireTableRowsUpdated(firstRow, lastRow);
    }

    int getRowCount() {
        return tableModelStatus.getRowCount();
    }

    Stream<SyncPair> getStream() {
        return tableModelStatus.getStream();
    }

    AbstractListTableModel<SyncPair> getTableModelReceiver() {
        return tableModelReceiver;
    }

    AbstractListTableModel<SyncPair> getTableModelSender() {
        return tableModelSender;
    }

    AbstractListTableModel<SyncPair> getTableModelStatus() {
        return tableModelStatus;
    }

    JTable getTableReceiver() {
        return tableReceiver;
    }

    JTable getTableSender() {
        return tableSender;
    }

    JTable getTableStatus() {
        return tableStatus;
    }

    void initRowSorter(final Supplier<Predicate<SyncPair>> supplierPredicate) {
        final RowFilter<AbstractListTableModel<SyncPair>, Integer> rowFilter = new RowFilter<>() {
            public boolean include(final Entry<? extends AbstractListTableModel<SyncPair>, ? extends Integer> entry) {
                final SyncPair syncPair = entry.getModel().getObjectAt(entry.getIdentifier());

                return supplierPredicate.get().test(syncPair);
            }
        };

        rowSorterSender = new TableRowSorter<>(tableModelSender);
        rowSorterSender.setRowFilter(rowFilter);
        tableSender.setRowSorter(rowSorterSender);

        rowSorterStatus = new TableRowSorter<>(tableModelStatus);
        rowSorterStatus.setRowFilter(rowFilter);
        tableStatus.setRowSorter(rowSorterStatus);

        rowSorterReceiver = new TableRowSorter<>(tableModelReceiver);
        rowSorterReceiver.setRowFilter(rowFilter);
        tableReceiver.setRowSorter(rowSorterReceiver);
    }

    void scrollToLastRow() {
        final int rowCount = getRowCount();

        Rectangle rectangle = tableSender.getCellRect(rowCount - 1, 0, false);
        tableSender.scrollRectToVisible(rectangle);

        rectangle = tableStatus.getCellRect(rowCount - 1, 0, false);
        tableStatus.scrollRectToVisible(rectangle);

        rectangle = tableReceiver.getCellRect(rowCount - 1, 0, false);
        tableReceiver.scrollRectToVisible(rectangle);
    }

    void sort() {
        rowSorterSender.sort();
        rowSorterStatus.sort();
        rowSorterReceiver.sort();
    }
}
