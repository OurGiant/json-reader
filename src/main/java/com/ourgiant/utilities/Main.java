package com.ourgiant.utilities;

import com.formdev.flatlaf.FlatLightLaf;
import com.ourgiant.utilities.gui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        if (!FlatLightLaf.setup()) {
            log.warn("FlatLaf setup failed; falling back to default look and feel");
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
