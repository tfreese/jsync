// Created: 29.07.2020
package de.freese.jsync.model;

import java.util.Comparator;
import java.util.Optional;

/**
 * @author Thomas Freese
 */
public class SyncPairComparator implements Comparator<SyncPair> {
    @Override
    public int compare(final SyncPair o1, final SyncPair o2) {
        String value1 = Optional.ofNullable(o1.getSenderItem()).map(SyncItem::getRelativePath).orElse(null);

        if (value1 == null) {
            value1 = Optional.ofNullable(o1.getReceiverItem()).map(SyncItem::getRelativePath).orElse(null);
        }

        if (value1 == null) {
            value1 = "";
        }

        String value2 = Optional.ofNullable(o2.getSenderItem()).map(SyncItem::getRelativePath).orElse(null);

        if (value2 == null) {
            value2 = Optional.ofNullable(o2.getReceiverItem()).map(SyncItem::getRelativePath).orElse(null);
        }

        if (value2 == null) {
            value2 = "";
        }

        return value1.compareTo(value2);
    }
}
