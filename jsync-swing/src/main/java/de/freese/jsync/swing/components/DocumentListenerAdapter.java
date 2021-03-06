//Created: 29.07.2020
package de.freese.jsync.swing.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author Thomas Freese
 */
public class DocumentListenerAdapter implements DocumentListener
{
    /**
     * Erstellt ein neues {@link DocumentListenerAdapter} Object.
     */
    public DocumentListenerAdapter()
    {
        super();
    }

    /**
     * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
     */
    @Override
    public void changedUpdate(final DocumentEvent e)
    {
        // Empty
    }

    /**
     * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
     */
    @Override
    public void insertUpdate(final DocumentEvent e)
    {
        // Empty
    }

    /**
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
     */
    @Override
    public void removeUpdate(final DocumentEvent e)
    {
        // Empty
    }
}
