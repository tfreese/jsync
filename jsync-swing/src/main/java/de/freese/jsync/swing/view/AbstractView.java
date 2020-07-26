/**
 * Created: 12.07.2020
 */

package de.freese.jsync.swing.view;

import javax.swing.JFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.swing.JSyncSwingApplication;

/**
 * @author Thomas Freese
 */
public abstract class AbstractView
{
    /**
    *
    */
    public final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Erstellt ein neues {@link AbstractView} Object.
     */
    public AbstractView()
    {
        super();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link JFrame}
     */
    protected JFrame getMainFrame()
    {
        return JSyncSwingApplication.getInstance().getMainFrame();
    }

    /**
     * @param key String
     * @return String
     */
    protected String getMessage(final String key)
    {
        return JSyncSwingApplication.getInstance().getMessages().getString(key);
    }
}
