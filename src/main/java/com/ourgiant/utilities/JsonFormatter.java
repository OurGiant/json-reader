package com.ourgiant.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
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
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class JsonFormatter extends JFrame {
   private JTextPane textPane;
   // private JTextArea lineNumberArea;
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

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      SwingUtilities.invokeLater(() -> {
         JsonFormatter var0 = new JsonFormatter();
         var0.setVisible(true);
      });
   }

   public JsonFormatter() {
      this.setTitle("JSON Formatter");
      this.setSize(900, 700);
      this.setDefaultCloseOperation(3);
      this.setTitle("JSON Tool v1.0");
      this.setSize(800, 600);
      this.setLocationRelativeTo((Component)null);
      this.initComponents();
      this.setupLayout();
      this.setupListeners();
      this.setupMenuBar();

      try {
         this.setIconImage(this.createAppIcon());
      } catch (Exception var2) {
      }

   }

   private Image createAppIcon() {
      BufferedImage var1 = new BufferedImage(16, 16, 1);
      Graphics2D var2 = var1.createGraphics();
      var2.setColor(new Color(70, 130, 180));
      var2.fillRect(0, 0, 16, 16);
      var2.setColor(Color.WHITE);
      var2.setFont(new Font("Arial", 1, 10));
      var2.drawString("J", 5, 12);
      var2.dispose();
      return var1;
   }

   private void initComponents() {
      this.textPane = new JTextPane();
      this.textPane.setFont(new Font("Consolas", 0, 14));
      this.textPane.setMargin(new Insets(5, 5, 5, 5));
      this.doc = this.textPane.getStyledDocument();
      // this.lineNumberArea = new JTextArea("1");
      // this.lineNumberArea.setFont(new Font("Consolas", 0, 14));
      // this.lineNumberArea.setBackground(new Color(240, 240, 240));
      // this.lineNumberArea.setForeground(new Color(100, 100, 100));
      // this.lineNumberArea.setEditable(false);
      // this.lineNumberArea.setMargin(new Insets(5, 5, 5, 5));
      // this.lineNumberArea.setFocusable(false);
      this.formatButton = new JButton("Format JSON");
      this.stringifyButton = new JButton("Stringify JSON");
      this.clearButton = new JButton("Clear");
      this.copyButton = new JButton("Copy");
      this.lintButton = new JButton("Lint");
      this.formatButton.setFont(new Font("Arial", 1, 12));
      this.stringifyButton.setFont(new Font("Arial", 1, 12));
      this.lintButton.setFont(new Font("Arial", 1, 12));
      this.clearButton.setFont(new Font("Arial", 0, 12));
      this.copyButton.setFont(new Font("Arial", 0, 12));
      this.statusLabel = new JLabel("Ready");
      this.statusLabel.setFont(new Font("Arial", 2, 11));
   }

   private void setupLayout() {
      this.setLayout(new BorderLayout(10, 10));
      JPanel var1 = new JPanel(new FlowLayout(0, 10, 10));
      var1.add(this.formatButton);
      var1.add(this.stringifyButton);
      var1.add(this.lintButton);
      var1.add(this.clearButton);
      var1.add(this.copyButton);
      this.add(var1, "North");
      JScrollPane var2 = new JScrollPane(this.textPane);
      // var2.setRowHeaderView(this.lineNumberArea);
      var2.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
      this.add(var2, "Center");
      JPanel var3 = new JPanel(new BorderLayout());
      var3.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      var3.add(this.statusLabel, "West");
      this.add(var3, "South");
   }

   private void setupMenuBar() {
      JMenuBar var1 = new JMenuBar();
      JMenu var2 = new JMenu("File");
      var2.setMnemonic(70);
      JMenuItem var3 = new JMenuItem("Save to File...");
      var3.setAccelerator(KeyStroke.getKeyStroke(83, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      var3.addActionListener((var1x) -> {
         this.saveToFile();
      });
      JMenuItem var4 = new JMenuItem("Exit");
      var4.setAccelerator(KeyStroke.getKeyStroke(81, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
      var4.addActionListener((var0) -> {
         System.exit(0);
      });
      var2.add(var3);
      var2.addSeparator();
      var2.add(var4);
      var1.add(var2);
      this.setJMenuBar(var1);
   }

   private void setupListeners() {
      this.textPane.getDocument().addDocumentListener(new DocumentListener() {
         private Timer timer = new Timer(800, (var1x) -> {
            this.autoProcess();
         });

         public void insertUpdate(DocumentEvent var1) {
            this.timer.restart();
            // JsonFormatter.this.updateLineNumbers();
         }

         public void changedUpdate(DocumentEvent var1) {
            // JsonFormatter.this.updateLineNumbers();
         }

         public void removeUpdate(DocumentEvent var1) {
            // JsonFormatter.this.updateLineNumbers();
         }

         private void autoProcess() {
            if (!JsonFormatter.this.isProcessing) {
               String var1 = JsonFormatter.this.textPane.getText().trim();
               if (var1.length() > 0) {
                  JsonFormatter.this.applySyntaxHighlighting(var1);
               }
            }

         }
      });
      this.formatButton.addActionListener((var1x) -> {
         this.formatJson();
      });
      this.stringifyButton.addActionListener((var1x) -> {
         this.stringifyJson();
      });
      this.lintButton.addActionListener((var1x) -> {
         this.lintJson();
      });
      this.clearButton.addActionListener((var1x) -> {
         this.textPane.setText("");
         // this.updateLineNumbers();
         this.statusLabel.setText("Ready");
      });
      this.copyButton.addActionListener((var1x) -> {
         this.copyToClipboard();
      });
      KeyStroke var1 = KeyStroke.getKeyStroke(70, 128);
      KeyStroke var2 = KeyStroke.getKeyStroke(83, 192);
      KeyStroke var3 = KeyStroke.getKeyStroke(76, 128);
      KeyStroke var4 = KeyStroke.getKeyStroke(75, 128);
      this.textPane.getInputMap().put(var1, "format");
      this.textPane.getActionMap().put("format", new AbstractAction() {
         public void actionPerformed(ActionEvent var1) {
            JsonFormatter.this.formatJson();
         }
      });
      this.textPane.getInputMap().put(var2, "stringify");
      this.textPane.getActionMap().put("stringify", new AbstractAction() {
         public void actionPerformed(ActionEvent var1) {
            JsonFormatter.this.stringifyJson();
         }
      });
      this.textPane.getInputMap().put(var3, "lint");
      this.textPane.getActionMap().put("lint", new AbstractAction() {
         public void actionPerformed(ActionEvent var1) {
            JsonFormatter.this.lintJson();
         }
      });
      this.textPane.getInputMap().put(var4, "clear");
      this.textPane.getActionMap().put("clear", new AbstractAction() {
         public void actionPerformed(ActionEvent var1) {
            JsonFormatter.this.textPane.setText("");
            // JsonFormatter.this.updateLineNumbers();
            JsonFormatter.this.statusLabel.setText("Ready");
         }
      });
   }

   // private void updateLineNumbers() {
   //    String var1 = this.textPane.getText();
   //    int var2 = 1;
   //    char[] var3 = var1.toCharArray();
   //    int var4 = var3.length;

   //    for(int var5 = 0; var5 < var4; ++var5) {
   //       char var6 = var3[var5];
   //       if (var6 == '\n') {
   //          ++var2;
   //       }
   //    }

   //    StringBuilder var7 = new StringBuilder();

   //    for(var4 = 1; var4 <= var2; ++var4) {
   //       var7.append(var4).append("\n");
   //    }

   //    this.lineNumberArea.setText(var7.toString());
   // }

   private void saveToFile() {
      JFileChooser var1 = new JFileChooser();
      var1.setDialogTitle("Save JSON File");
      var1.setSelectedFile(new File("output.json"));
      int var2 = var1.showSaveDialog(this);
      if (var2 == 0) {
         File var3 = var1.getSelectedFile();

         try {
            BufferedWriter var4 = new BufferedWriter(new FileWriter(var3));

            try {
               var4.write(this.textPane.getText());
               this.statusLabel.setText("Saved to: " + var3.getName());
               this.statusLabel.setForeground(new Color(0, 128, 0));
               Timer var5 = new Timer(3000, (var1x) -> {
                  this.statusLabel.setText("Ready");
                  this.statusLabel.setForeground(Color.BLACK);
               });
               var5.setRepeats(false);
               var5.start();
            } catch (Throwable var8) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }

               throw var8;
            }

            var4.close();
         } catch (IOException var9) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + var9.getMessage(), "Error", 0);
         }
      }

   }

   private void lintJson() {
      String var1 = this.textPane.getText().trim();
      if (var1.isEmpty()) {
         this.statusLabel.setText("Nothing to lint");
      } else {
         try {
            ArrayList<String> var2 = new ArrayList<>();
            this.validateJson(var1, var2);
            if (var2.isEmpty()) {
               this.statusLabel.setText("Valid JSON - No issues found");
               this.statusLabel.setForeground(new Color(0, 128, 0));
               JOptionPane.showMessageDialog(this, "Valid JSON\nNo issues found!", "Lint Result", 1);
            } else {
               this.statusLabel.setText(var2.size() + " issue(s) found");
               this.statusLabel.setForeground(new Color(204, 102, 0));
               StringBuilder var3 = new StringBuilder("Issues found:\n\n");
               Iterator<String> var4 = var2.iterator();

               while(var4.hasNext()) {
                  String var5 = var4.next();
                  var3.append("- ").append(var5).append("\n");
               }

               JOptionPane.showMessageDialog(this, var3.toString(), "Lint Result", 2);
            }

            Timer var7 = new Timer(5000, (var1x) -> {
               this.statusLabel.setText("Ready");
               this.statusLabel.setForeground(Color.BLACK);
            });
            var7.setRepeats(false);
            var7.start();
         } catch (Exception var6) {
            this.statusLabel.setText("Invalid JSON");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON:\n" + var6.getMessage(), "Lint Error", 0);
         }

      }
   }

   private void validateJson(String var1, List<String> var2) throws Exception {
      var1 = var1.trim();
      if (var1.startsWith("\"") && var1.endsWith("\"")) {
         var1 = this.unescapeJson(var1.substring(1, var1.length() - 1));
      }

      if (!var1.startsWith("{") && !var1.startsWith("[")) {
         throw new Exception("JSON must start with { or [");
      } else {
         Stack var3 = new Stack();
         boolean var4 = false;
         boolean var5 = false;
         int var6 = 1;

         for(int var7 = 0; var7 < var1.length(); ++var7) {
            char var8 = var1.charAt(var7);
            if (var8 == '\n') {
               ++var6;
            }

            if (var5) {
               var5 = false;
            } else if (var8 == '\\') {
               var5 = true;
            } else if (var8 == '"') {
               var4 = !var4;
            } else if (!var4) {
               if (var8 != '{' && var8 != '[') {
                  if (var8 == '}' || var8 == ']') {
                     if (var3.isEmpty()) {
                        throw new Exception("Unmatched closing bracket at line " + var6);
                     }

                     char var9 = (Character)var3.pop();
                     if (var8 == '}' && var9 != '{' || var8 == ']' && var9 != '[') {
                        var2.add("Mismatched brackets at line " + var6);
                     }
                  }
               } else {
                  var3.push(var8);
               }
            }
         }

         if (!var3.isEmpty()) {
            throw new Exception("Unclosed brackets");
         } else {
            if (var1.matches(".*,\\s*[}\\]].*")) {
               var2.add("Trailing comma detected (not allowed in strict JSON)");
            }

         }
      }
   }

   private void formatJson() {
      this.isProcessing = true;
      String var1 = this.textPane.getText().trim();
      if (var1.isEmpty()) {
         this.isProcessing = false;
      } else {
         try {
            String var2 = this.formatJsonString(var1);
            this.textPane.setText(var2);
            this.applySyntaxHighlighting(var2);
            // this.updateLineNumbers();
            this.textPane.setCaretPosition(0);
            this.statusLabel.setText("Formatted successfully");
         } catch (Exception var6) {
            this.statusLabel.setText("Error formatting");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON: " + var6.getMessage(), "Error", 0);
         } finally {
            this.isProcessing = false;
         }

      }
   }

   private void stringifyJson() {
      this.isProcessing = true;
      String var1 = this.textPane.getText().trim();
      if (var1.isEmpty()) {
         this.isProcessing = false;
      } else {
         try {
            String var2 = this.compactJsonString(var1);
            String var3 = this.stringifyJsonString(var2);
            this.textPane.setText(var3);
            this.applySyntaxHighlighting(var3);
            // this.updateLineNumbers();
            this.textPane.setCaretPosition(0);
            this.statusLabel.setText("Stringified successfully");
         } catch (Exception var7) {
            this.statusLabel.setText("Error stringifying");
            this.statusLabel.setForeground(COLOR_ERROR);
            JOptionPane.showMessageDialog(this, "Invalid JSON: " + var7.getMessage(), "Error", 0);
         } finally {
            this.isProcessing = false;
         }

      }
   }

   private void applySyntaxHighlighting(String var1) {
      SwingUtilities.invokeLater(() -> {
         try {
            Style var2 = this.textPane.addStyle("default", (Style)null);
            StyleConstants.setForeground(var2, Color.BLACK);
            this.doc.setCharacterAttributes(0, this.doc.getLength(), var2, true);
            boolean var3 = false;
            boolean var4 = false;
            boolean var5 = false;
            int var6 = -1;
            StringBuilder var7 = new StringBuilder();
            int var8 = -1;

            for(int var9 = 0; var9 < var1.length(); ++var9) {
               char var10 = var1.charAt(var9);
               if (var4) {
                  var4 = false;
               } else if (var10 == '\\') {
                  var4 = true;
               } else if (var10 == '"') {
                  if (var3) {
                     this.applyColor(var6, var9 + 1 - var6, var5 ? COLOR_KEY : COLOR_STRING);
                     var3 = false;
                     var5 = false;
                  } else {
                     var6 = var9;
                     var3 = true;

                     for(int var11 = var9 + 1; var11 < var1.length(); ++var11) {
                        if (var1.charAt(var11) == ':') {
                           var5 = true;
                           break;
                        }

                        if (!Character.isWhitespace(var1.charAt(var11)) && var1.charAt(var11) != '"') {
                           break;
                        }
                     }
                  }
               } else if (!var3) {
                  if (var10 != '{' && var10 != '}' && var10 != '[' && var10 != ']' && var10 != ',' && var10 != ':') {
                     if (!Character.isWhitespace(var10)) {
                        if (var8 == -1) {
                           var8 = var9;
                        }

                        var7.append(var10);
                     } else if (var7.length() > 0) {
                        this.highlightToken(var7.toString(), var8);
                        var7 = new StringBuilder();
                        var8 = -1;
                     }
                  } else {
                     if (var7.length() > 0) {
                        this.highlightToken(var7.toString(), var8);
                        var7 = new StringBuilder();
                     }

                     this.applyColor(var9, 1, COLOR_BRACE);
                     var8 = -1;
                  }
               }
            }

            if (var7.length() > 0) {
               this.highlightToken(var7.toString(), var8);
            }
         } catch (Exception var12) {
         }

      });
   }

   private void highlightToken(String var1, int var2) {
      Color var3 = COLOR_BRACE;
      if (!var1.equals("true") && !var1.equals("false")) {
         if (var1.equals("null")) {
            var3 = COLOR_NULL;
         } else if (var1.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
            var3 = COLOR_NUMBER;
         }
      } else {
         var3 = COLOR_BOOLEAN;
      }

      this.applyColor(var2, var1.length(), var3);
   }

   private void applyColor(int var1, int var2, Color var3) {
      Style var4 = this.textPane.addStyle("temp", (Style)null);
      StyleConstants.setForeground(var4, var3);
      this.doc.setCharacterAttributes(var1, var2, var4, false);
   }

   private void copyToClipboard() {
      String var1 = this.textPane.getText();
      if (!var1.isEmpty()) {
         StringSelection var2 = new StringSelection(var1);
         Clipboard var3 = Toolkit.getDefaultToolkit().getSystemClipboard();
         var3.setContents(var2, var2);
         this.statusLabel.setText("Copied to clipboard");
      }

   }

   private String formatJsonString(String var1) throws Exception {
      var1 = var1.trim();
      if (var1.startsWith("\"") && var1.endsWith("\"")) {
         var1 = this.unescapeJson(var1.substring(1, var1.length() - 1));
      }

      StringBuilder var2 = new StringBuilder();
      int var3 = 0;
      boolean var4 = false;
      boolean var5 = false;

      for(int var6 = 0; var6 < var1.length(); ++var6) {
         char var7 = var1.charAt(var6);
         if (var5) {
            var2.append(var7);
            var5 = false;
         } else if (var7 == '\\') {
            var2.append(var7);
            var5 = true;
         } else if (var7 == '"') {
            var4 = !var4;
            var2.append(var7);
         } else if (var4) {
            var2.append(var7);
         } else {
            switch (var7) {
               case '\t':
               case '\n':
               case '\r':
               case ' ':
                  break;
               case ',':
                  var2.append(var7).append('\n');
                  var2.append("  ".repeat(var3));
                  break;
               case ':':
                  var2.append(var7).append(' ');
                  break;
               case '[':
               case '{':
                  var2.append(var7).append('\n');
                  ++var3;
                  var2.append("  ".repeat(var3));
                  break;
               case ']':
               case '}':
                  var2.append('\n');
                  --var3;
                  var2.append("  ".repeat(var3));
                  var2.append(var7);
                  break;
               default:
                  var2.append(var7);
            }
         }
      }

      return var2.toString();
   }

   private String compactJsonString(String var1) throws Exception {
      var1 = var1.trim();
      if (var1.startsWith("\"") && var1.endsWith("\"")) {
         var1 = this.unescapeJson(var1.substring(1, var1.length() - 1));
      }

      StringBuilder var2 = new StringBuilder();
      boolean var3 = false;
      boolean var4 = false;

      for(int var5 = 0; var5 < var1.length(); ++var5) {
         char var6 = var1.charAt(var5);
         if (var4) {
            var2.append(var6);
            var4 = false;
         } else if (var6 == '\\') {
            var2.append(var6);
            var4 = true;
         } else if (var6 == '"') {
            var3 = !var3;
            var2.append(var6);
         } else if (var3) {
            var2.append(var6);
         } else if (var6 != ' ' && var6 != '\n' && var6 != '\r' && var6 != '\t') {
            var2.append(var6);
         }
      }

      return var2.toString();
   }

   private String stringifyJsonString(String var1) {
      String var10000 = this.escapeJson(var1);
      return "\"" + var10000 + "\"";
   }

   private String escapeJson(String var1) {
      return var1.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   private String unescapeJson(String var1) {
      return var1.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
   }

}
