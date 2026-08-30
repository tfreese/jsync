package de.freese.jsync.swing.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author Thomas Freese
 * @since 29.07.2020
 */
public class DocumentListenerAdapter implements DocumentListener {
    @Override
    public void changedUpdate(final DocumentEvent event) {
        // Empty
    }

    @Override
    public void insertUpdate(final DocumentEvent event) {
        // Empty
    }

    @Override
    public void removeUpdate(final DocumentEvent event) {
        // Empty
    }
}
