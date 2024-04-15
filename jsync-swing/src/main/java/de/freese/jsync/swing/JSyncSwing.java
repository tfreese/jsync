// Created: 25.11.2018
package de.freese.jsync.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.plaf.FontUIResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.swing.controller.JSyncController;
import de.freese.jsync.swing.view.DefaultSyncView;
import de.freese.jsync.swing.view.SyncView;

/**
 * @author Thomas Freese
 */
public final class JSyncSwing {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncSwing.class);

    public static Logger getLogger() {
        return LOGGER;
    }

    private final class MainFrameListener extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent event) {
            getLogger().info("stop");
            JSyncSwing.this.controller.shutdown();
            JSyncContext.shutdown();

            System.exit(0);
        }
    }

    private JSyncController controller;

    JSyncSwing() {
        super();
    }

    void initGui(final Consumer<JFrame> consumer) throws Exception {
        initUIDefaults();

        final JFrame frame = new JFrame();
        frame.setTitle(JSyncContext.getMessages().getString("jsync.title"));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new MainFrameListener());
        frame.setLayout(new BorderLayout());

        final SyncView syncView = new DefaultSyncView();
        syncView.initGui();

        this.controller = new JSyncController();
        this.controller.init(syncView);

        // JLabel label = new JLabel("jSync Swing GUI", SwingConstants.CENTER);
        // frame.add(label, BorderLayout.CENTER);
        frame.add(syncView.getComponent(), BorderLayout.CENTER);

        consumer.accept(frame);
    }

    private void initUIDefaults() {
        getLogger().info("initUIDefaults");

        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // com.sun.java.swing.plaf.gtk.GTKLookAndFeel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            getLogger().error(ex.getMessage(), ex);
        }

        UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

        final Color color = new Color(215, 215, 215);
        // UIManager.put("Table.alternatingBackground", color);
        UIManager.put("Table.alternateRowColor", color);
        // UIManager.put("List.alternatingBackground", color);
        // defaults.put("Tree.alternatingBackground", color);

        // Fonts: Dialog, Monospaced, Arial, DejaVu Sans
        final Font font = new Font("DejaVu Sans", Font.PLAIN, 16);

        UIManager.getLookAndFeelDefaults().forEach((key, value) -> {
            if (value instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font));
            }

            // String keyString = key.toString();
            //
            // if (keyString.endsWith(".font") || keyString.endsWith(".acceleratorFont")) {
            // UIManager.put(key, new FontUIResource(font));
            // }
        });

        // final Font fontBold = font.deriveFont(Font.BOLD);
        // UIManager.put("TitledBorder.font", fontBold);

        // UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        //
        // SortedSet<Object> uiKeys = new TreeSet<>(new Comparator<Object>() {
        // @Override
        // public int compare(final Object o1, final Object o2) {
        // return o1.toString().compareTo(o2.toString());
        // }
        //
        // });
        // uiKeys.addAll(defaults.keySet());
        //
        // String format = "%1$s \t %2$s %n";
        //
        // for (Object key : uiKeys) {
        // Object value = defaults.get(key);
        //
        // System.out.printf(format, key.toString(), (value != null) ? value.toString() : "NULL");
        // }
    }
}
