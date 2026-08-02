package com.ourgiant.utilities.gui;

import com.ourgiant.utilities.core.JsonProcessingException;
import com.ourgiant.utilities.core.JsonProcessor;
import com.ourgiant.utilities.model.JsonToken;
import com.ourgiant.utilities.util.AppVersion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MainWindow extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private JTextPane textPane;
    private JButton formatButton;
    private JButton stringifyButton;
    private JButton clearButton;
    private JButton copyButton;
    private JButton lintButton;
    private JLabel statusLabel;
    private boolean isProcessing = false;
    private StyledDocument doc;
    private static final Color COLOR_KEY = new Color(0, 102, 204);
    private static final Color COLOR_STRING = new Color(0, 153, 0);
    private static final Color COLOR_NUMBER = new Color(153, 0, 153);
    private static final Color COLOR_BOOLEAN = new Color(204, 102, 0);
    private static final Color COLOR_NULL = new Color(128, 128, 128);
    private static final Color COLOR_BRACE = new Color(51, 51, 51);
    private static final Color COLOR_ERROR = new Color(204, 0, 0);

    public MainWindow() {
        this.setTitle("JSON Tool v" + AppVersion.resolve());
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo((Component) null);
        this.initComponents();
        this.setupLayout();
        this.setupListeners();
        this.setupMenuBar();

        try {
            this.setIconImage(this.createAppIcon());
        } catch (Exception ex) {
            log.warn("Failed to create app icon", ex);
        }
    }

    private Image createAppIcon() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(70, 130, 180));
        g.fillRect(0, 0, 16, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("J", 5, 12);
        g.dispose();
        return icon;
    }

    private void initComponents() {
        this.textPane = new JTextPane();
        this.textPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        this.textPane.setMargin(new Insets(5, 5, 5, 5));
        this.doc = this.textPane.getStyledDocument();
        this.formatButton = new JButton("Format JSON");
        this.stringifyButton = new JButton("Stringify JSON");
        this.clearButton = new JButton("Clear");
        this.copyButton = new JButton("Copy");
        this.lintButton = new JButton("Lint");
        this.formatButton.setFont(new Font("Arial", Font.BOLD, 12));
        this.stringifyButton.setFont(new Font("Arial", Font.BOLD, 12));
        this.lintButton.setFont(new Font("Arial", Font.BOLD, 12));
        this.clearButton.setFont(new Font("Arial", Font.PLAIN, 12));
        this.copyButton.setFont(new Font("Arial", Font.PLAIN, 12));
        this.statusLabel = new JLabel("Ready");
        this.statusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
    }

    private void setupLayout() {
        this.setLayout(new BorderLayout(10, 10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.add(this.formatButton);
        buttonPanel.add(this.stringifyButton);
        buttonPanel.add(this.lintButton);
        buttonPanel.add(this.clearButton);
        buttonPanel.add(this.copyButton);
        this.add(buttonPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(this.textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        this.add(scrollPane, BorderLayout.CENTER);
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(this.statusLabel, BorderLayout.WEST);
        this.add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        JMenuItem saveItem = new JMenuItem("Save to File...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> this.saveToFile());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        this.setJMenuBar(menuBar);
    }

    private void setupListeners() {
        this.textPane.getDocument().addDocumentListener(new DocumentListener() {
            private final Timer timer = new Timer(800, e -> this.autoProcess());

            public void insertUpdate(DocumentEvent e) {
                this.timer.restart();
            }

            public void changedUpdate(DocumentEvent e) {
            }

            public void removeUpdate(DocumentEvent e) {
            }

            private void autoProcess() {
                if (!MainWindow.this.isProcessing) {
                    String text = MainWindow.this.textPane.getText().trim();
                    if (!text.isEmpty()) {
                        MainWindow.this.applySyntaxHighlighting(text);
                    }
                }
            }
        });
        this.formatButton.addActionListener(e -> this.formatJson());
        this.stringifyButton.addActionListener(e -> this.stringifyJson());
        this.lintButton.addActionListener(e -> this.lintJson());
        this.clearButton.addActionListener(e -> {
            this.textPane.setText("");
            this.statusLabel.setText("Ready");
        });
        this.copyButton.addActionListener(e -> this.copyToClipboard());

        KeyStroke formatKey = KeyStroke.getKeyStroke('F', java.awt.event.InputEvent.CTRL_DOWN_MASK);
        KeyStroke stringifyKey = KeyStroke.getKeyStroke('S', java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
        KeyStroke lintKey = KeyStroke.getKeyStroke('L', java.awt.event.InputEvent.CTRL_DOWN_MASK);
        KeyStroke clearKey = KeyStroke.getKeyStroke('K', java.awt.event.InputEvent.CTRL_DOWN_MASK);

        this.textPane.getInputMap().put(formatKey, "format");
        this.textPane.getActionMap().put("format", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.formatJson();
            }
        });
        this.textPane.getInputMap().put(stringifyKey, "stringify");
        this.textPane.getActionMap().put("stringify", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.stringifyJson();
            }
        });
        this.textPane.getInputMap().put(lintKey, "lint");
        this.textPane.getActionMap().put("lint", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.lintJson();
            }
        });
        this.textPane.getInputMap().put(clearKey, "clear");
        this.textPane.getActionMap().put("clear", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MainWindow.this.textPane.setText("");
                MainWindow.this.statusLabel.setText("Ready");
            }
        });
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save JSON File");
        chooser.setSelectedFile(new File("output.json"));
        int choice = chooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(this.textPane.getText());
                this.statusLabel.setText("Saved to: " + file.getName());
                this.statusLabel.setForeground(new Color(0, 128, 0));
                Timer resetTimer = new Timer(3000, e -> {
                    this.statusLabel.setText("Ready");
                    this.statusLabel.setForeground(Color.BLACK);
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            } catch (IOException ex) {
                log.error("Failed to save JSON to {}", file, ex);
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void lintJson() {
        String text = this.textPane.getText().trim();
        if (text.isEmpty()) {
            this.statusLabel.setText("Nothing to lint");
            return;
        }
        try {
            List<String> warnings = new ArrayList<>();
            JsonProcessor.validate(text, warnings);
            if (warnings.isEmpty()) {
                this.statusLabel.setText("Valid JSON - No issues found");
                this.statusLabel.setForeground(new Color(0, 128, 0));
                JOptionPane.showMessageDialog(this, "Valid JSON\nNo issues found!", "Lint Result", JOptionPane.INFORMATION_MESSAGE);
            } else {
                this.statusLabel.setText(warnings.size() + " issue(s) found");
                this.statusLabel.setForeground(new Color(204, 102, 0));
                StringBuilder message = new StringBuilder("Issues found:\n\n");
                for (String warning : warnings) {
                    message.append("- ").append(warning).append("\n");
                }
                JOptionPane.showMessageDialog(this, message.toString(), "Lint Result", JOptionPane.WARNING_MESSAGE);
            }

            Timer resetTimer = new Timer(5000, e -> {
                this.statusLabel.setText("Ready");
                this.statusLabel.setForeground(Color.BLACK);
            });
            resetTimer.setRepeats(false);
            resetTimer.start();
        } catch (JsonProcessingException ex) {
            this.statusLabel.setText("Invalid JSON");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON:\n" + ex.getMessage(), "Lint Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void formatJson() {
        this.isProcessing = true;
        String text = this.textPane.getText().trim();
        if (text.isEmpty()) {
            this.isProcessing = false;
            return;
        }
        try {
            String formatted = JsonProcessor.format(text);
            this.textPane.setText(formatted);
            this.applySyntaxHighlighting(formatted);
            this.textPane.setCaretPosition(0);
            this.statusLabel.setText("Formatted successfully");
        } catch (Exception ex) {
            this.statusLabel.setText("Error formatting");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            this.isProcessing = false;
        }
    }

    private void stringifyJson() {
        this.isProcessing = true;
        String text = this.textPane.getText().trim();
        if (text.isEmpty()) {
            this.isProcessing = false;
            return;
        }
        try {
            String compacted = JsonProcessor.compact(text);
            String stringified = JsonProcessor.stringify(compacted);
            this.textPane.setText(stringified);
            this.applySyntaxHighlighting(stringified);
            this.textPane.setCaretPosition(0);
            this.statusLabel.setText("Stringified successfully");
        } catch (Exception ex) {
            this.statusLabel.setText("Error stringifying");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            this.isProcessing = false;
        }
    }

    private void applySyntaxHighlighting(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                Style defaultStyle = this.textPane.addStyle("default", null);
                StyleConstants.setForeground(defaultStyle, Color.BLACK);
                this.doc.setCharacterAttributes(0, this.doc.getLength(), defaultStyle, true);
                for (JsonToken token : JsonProcessor.tokenize(text)) {
                    this.applyColor(token.start(), token.length(), colorFor(token.type()));
                }
            } catch (Exception ex) {
                log.debug("Syntax highlighting skipped for in-progress input", ex);
            }
        });
    }

    private static Color colorFor(com.ourgiant.utilities.model.JsonTokenType type) {
        return switch (type) {
            case KEY -> COLOR_KEY;
            case STRING -> COLOR_STRING;
            case NUMBER -> COLOR_NUMBER;
            case BOOLEAN -> COLOR_BOOLEAN;
            case NULL -> COLOR_NULL;
            case PUNCTUATION -> COLOR_BRACE;
        };
    }

    private void applyColor(int start, int length, Color color) {
        Style style = this.textPane.addStyle("temp", null);
        StyleConstants.setForeground(style, color);
        this.doc.setCharacterAttributes(start, length, style, false);
    }

    private void copyToClipboard() {
        String text = this.textPane.getText();
        if (!text.isEmpty()) {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            this.statusLabel.setText("Copied to clipboard");
        }
    }
}
