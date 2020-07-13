/**
 * Created: 11.07.2020
 */

package de.freese.jsync.swing.view;

import java.awt.Color;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import de.freese.jsync.swing.GbcBuilder;

/**
 * @author Thomas Freese
 */
public class SyncView extends AbstractView
{
    /**
     *
     */
    private JButton buttonCompare = null;

    /**
     *
     */
    private JButton buttonSyncronize;

    /**
     *
     */
    private JCheckBox checkBoxChecksum = null;

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

        // Button Compare
        this.buttonCompare = new JButton(getMessage("jsync.vergleichen"));
        confiPanel.add(this.buttonCompare, new GbcBuilder(0, 0).insets(5, 5, 5, 20));
        this.buttonCompare.setEnabled(true);

        // Optionen
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new GridBagLayout());
        panelOptions.setBorder(new TitledBorder(getMessage("jsync.optionen")));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        this.checkBoxChecksum = new JCheckBox(getMessage("jsync.pruefsumme"), false);
        panelOptions.add(this.checkBoxChecksum, new GbcBuilder(0, 0));
        confiPanel.add(panelOptions, new GbcBuilder(1, 0));

        // Button Synchronize
        this.buttonSyncronize = new JButton(getMessage("jsync.synchronisieren"));
        confiPanel.add(this.buttonSyncronize, new GbcBuilder(2, 0).insets(5, 20, 5, 5));
        this.buttonSyncronize.setEnabled(false);

        return confiPanel;
    }

    /**
     * @return {@link JButton}
     */
    public JButton getButtonCompare()
    {
        return this.buttonCompare;
    }

    /**
     * @return {@link JButton}
     */
    public JButton getButtonSyncronize()
    {
        return this.buttonSyncronize;
    }

    /**
     * @return {@link JCheckBox}
     */
    public JCheckBox getCheckBoxChecksum()
    {
        return this.checkBoxChecksum;
    }

    /**
     * @return {@link JPanel}
     */
    public JPanel getPanel()
    {
        return this.panel;
    }

    /**
     * @return {@link ReceiverView}
     */
    public ReceiverView getReceiverView()
    {
        return this.receiverView;
    }

    /**
     * @return {@link SenderView}
     */
    public SenderView getSenderView()
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
        this.panel.add(configPanel, new GbcBuilder(0, 0).gridwidth(3).anchorCenter().fillHorizontal());

        this.senderView.initGUI();
        this.receiverView.initGUI();

        this.panel.add(this.senderView.getPanel(), new GbcBuilder(0, 1).fillBoth());
        this.panel.add(new JPanel(), new GbcBuilder(1, 1).fillVertical());
        this.panel.add(this.receiverView.getPanel(), new GbcBuilder(2, 1).fillBoth());
    }
}
