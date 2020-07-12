/**
 * Created: 11.07.2020
 */

package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import de.freese.jsync.swing.GbcBuilder;

/**
 * @author Thomas Freese
 */
public class SyncView
{
    /**
     *
     */
    private final JPanel panel;

    /**
       *
       */
    private final ReceiverView receiverView;

    /**
    *
    */
    private final SenderView senderView;

    /**
     * Erstellt ein neues {@link SyncView} Object.
     */
    public SyncView()
    {
        super();

        this.panel = new JPanel();
        this.senderView = new SenderView();
        this.receiverView = new ReceiverView();
    }

    /**
     * @return {@link JPanel}
     */
    private JPanel createConfigPanel()
    {
        JPanel confiPanel = new JPanel();
        confiPanel.setLayout(new GridBagLayout());
        confiPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        confiPanel.add(new JLabel("Konfiguration"), new GbcBuilder(0, 0));

        return confiPanel;
    }

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     * @return {@link AbstractFileSystemView}
     */
    public AbstractFileSystemView getReceiverView()
    {
        return this.receiverView;
    }

    /**
     * @return {@link AbstractFileSystemView}
     */
    public AbstractFileSystemView getSenderView()
    {
        return this.senderView;
    }

    /**
     *
     */
    public void initGUI()
    {
        this.panel.setLayout(new GridBagLayout());

        JPanel configPanel = createConfigPanel();
        this.panel.add(configPanel, new GbcBuilder(0, 0).gridwidth(2).anchorCenter().fillHorizontal());

        this.senderView.initGUI();
        this.receiverView.initGUI();

        this.panel.add(this.senderView.getPanel(), new GbcBuilder(0, 1).fillBoth());
        this.panel.add(this.receiverView.getPanel(), new GbcBuilder(1, 1).fillBoth());
    }
}
