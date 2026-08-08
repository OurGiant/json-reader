package com.ourgiant.utilities;

import com.ourgiant.utilities.gui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            AppPreferences preferences = new AppPreferences();
            if (!ThemeManager.applyTheme(preferences.getTheme())) {
                log.warn("Failed to apply saved theme; falling back to default look and feel");
            }

            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
