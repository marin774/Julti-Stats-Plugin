package me.marin.statsplugin.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import me.marin.statsplugin.GoogleSheets;
import me.marin.statsplugin.StatsPlugin;
import me.marin.statsplugin.StatsPluginUtil;
import me.marin.statsplugin.io.StatsPluginSettings;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.util.ExceptionUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

import static me.marin.statsplugin.StatsPlugin.GOOGLE_SHEETS_CREDENTIALS_PATH;

public class SetupGUI extends JFrame {

    // Card1 - do you want google sheets
    // Card2 - have you used tracker before
    // Card3 - import credentials
    // Card4 - setup google sheet + import credentials
    // Card5 - setup complete

    private static final String DEFAULT_CARD = "Card1";
    private static final Map<String, String> previousButtonCardMap = new HashMap<>();

    static {
        previousButtonCardMap.put("Card2", "Card1");
        previousButtonCardMap.put("Card3", "Card2");
        previousButtonCardMap.put("Card4", "Card2");
    }

    private static final Map<String, String> nextButtonCardMap = new HashMap<>();

    static {
        nextButtonCardMap.put("Card3", "Card5");
        nextButtonCardMap.put("Card4", "Card5");
    }

    private boolean isClosed = false;
    private String currentCard;

    private JPanel cardContainer;
    private JPanel navigationBar;
    private JButton backButton;
    private JButton exitButton;
    private JPanel card1;
    private JButton card1_yesButton;
    private JButton card1_noButton;
    private JPanel card2;
    private JButton card2_noButton;
    private JButton card2_yesButton;
    private JPanel mainPanel;
    private JEditorPane card1_jep;
    private JEditorPane card2_jep;
    private JPanel card3;
    private JButton nextButton;
    private JButton card3_importButton;
    private JLabel card3_importLabel;
    private JTextField card3_sheetURLField;
    private JButton card3_testButton;
    private JPanel card4;
    private JEditorPane card4_jep;
    private JButton card4_importButton;
    private JLabel card4_importLabel;
    private JTextField card4_sheetURLField;
    private JButton card4_testButton;
    private JButton card4_copyEmailButton;
    private JButton finishButton;
    private JEditorPane card5_jep;

    private Path credentialsFilePath;
    private String gcEmail;
    private String googleSheetsId;

    public SetupGUI() {
        $$$setupUI$$$();
        modifyUIComponents();
        this.setContentPane(mainPanel);
        this.setTitle("Quick Setup - Stats Plugin");
        this.pack();
        this.setVisible(true);
        this.setResizable(false);
        this.setLocation(JultiGUI.getJultiGUI().getLocation());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isClosed = true;
            }
        });

    }

    private void createUIComponents() {

    }

    private void modifyUIComponents() {
        setCurrentCard(DEFAULT_CARD);

        // Clickable links
        List<JEditorPane> editorPanes = new ArrayList<>();
        editorPanes.add(card1_jep);
        editorPanes.add(card2_jep);
        editorPanes.add(card4_jep);
        editorPanes.add(card5_jep);
        for (JEditorPane jep : editorPanes) {
            jep.addHyperlinkListener(hle -> {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    try {
                        Desktop.getDesktop().browse(URI.create(String.valueOf(hle.getURL())));
                    } catch (Exception e) {
                        Julti.log(Level.ERROR, "Failed to open link:\n" + ExceptionUtil.toDetailedString(e));
                    }
                }
            });
        }

        card1_yesButton.addActionListener(a -> {
            setCurrentCard("Card2");
        });
        card1_noButton.addActionListener(a -> {
            setCurrentCard("Card5");
        });

        card2_yesButton.addActionListener(a -> {
            setCurrentCard("Card3");
        });
        card2_noButton.addActionListener(a -> {
            setCurrentCard("Card4");
        });

        card3_importButton.addActionListener(importButtonLogic(card3_importLabel, card3_sheetURLField, card3_testButton, null));
        card3_sheetURLField.getDocument().addDocumentListener(sheetURLFieldLogic(card3_testButton));
        card3_testButton.addActionListener(testButtonLogic());

        card4_importButton.addActionListener(importButtonLogic(card4_importLabel, card4_sheetURLField, card4_testButton, card4_copyEmailButton));
        card4_sheetURLField.getDocument().addDocumentListener(sheetURLFieldLogic(card4_testButton));
        card4_testButton.addActionListener(testButtonLogic());
        card4_copyEmailButton.addActionListener(a -> {
            if (gcEmail == null) return;
            StringSelection stringSelection = new StringSelection(gcEmail);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            JOptionPane.showMessageDialog(null, "Copied email to clipboard.");
        });

        backButton.addActionListener(a -> {
            if (previousButtonCardMap.containsKey(getCurrentCard())) {
                setCurrentCard(previousButtonCardMap.get(getCurrentCard()));
            }
        });
        nextButton.addActionListener(a -> {
            if (nextButtonCardMap.containsKey(getCurrentCard())) {
                setCurrentCard(nextButtonCardMap.get(getCurrentCard()));
            }
        });

        finishButton.addActionListener(a -> {
            if (credentialsFilePath != null) {
                try {
                    Files.copy(credentialsFilePath, GOOGLE_SHEETS_CREDENTIALS_PATH, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    Julti.log(Level.DEBUG, ExceptionUtil.toDetailedString(e));
                    JOptionPane.showMessageDialog(null, "Looks like you deleted the credentials file before the setup was over. Please run the setup again.");
                    return;
                }

                if (googleSheetsId != null) {
                    StatsPluginSettings.getInstance().sheetLink = "https://docs.google.com/spreadsheets/d/" + googleSheetsId;
                }

                StatsPluginSettings.getInstance().useSheets = true;

                StatsPlugin.reloadGoogleSheets();
            }
            StatsPluginSettings.getInstance().completedSetup = true;
            StatsPluginSettings.getInstance().trackerEnabled = true;
            StatsPluginSettings.save();

            this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));

            StatsPlugin.statsGUI.updateGUI();
        });
    }

    private void setCurrentCard(String card) {
        CardLayout layout = (CardLayout) cardContainer.getLayout();
        layout.show(cardContainer, card);

        backButton.setEnabled(previousButtonCardMap.containsKey(card));

        this.currentCard = card;

        if (currentCard.equals("Card5")) {
            navigationBar.setVisible(false);
        }
    }

    public String getCurrentCard() {
        return this.currentCard;
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    private ActionListener importButtonLogic(JLabel importLabel, JTextField sheetURLField, JButton testButton, JButton copyEmailButton) {
        return (a) -> {
            try {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*.json", "json");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    /*
                    if (Files.exists(GOOGLE_SHEETS_CREDENTIALS_PATH)) {
                        // file exists, overwrite if user wants
                        int option = JOptionPane.showOptionDialog(
                                null,
                                "Looks like you already imported this file. Do you want to overwrite it?",
                                "Warning",
                                YES_NO_OPTION,
                                WARNING_MESSAGE,
                                null,
                                null,
                                "Yes");

                        Julti.log(Level.INFO, "CLICKED ON " + option);

                        if (option == 0) {
                            Files.copy(file.toPath(), GOOGLE_SHEETS_CREDENTIALS_PATH, StandardCopyOption.REPLACE_EXISTING);
                        } else if (option != 1) {
                            return;
                        }
                    } else {
                        Files.copy(file.toPath(), GOOGLE_SHEETS_CREDENTIALS_PATH, StandardCopyOption.REPLACE_EXISTING);
                    }*/

                    try (JsonReader reader = new JsonReader(new FileReader(file))) {
                        JsonObject object = new Gson().fromJson(reader, JsonObject.class);
                        boolean isValid = object.has("type") && object.get("type").getAsString().equals("service_account") && object.has("client_email");
                        if (!isValid) {
                            importLabel.setText("Invalid credentials file.");
                            sheetURLField.setEnabled(false);
                            testButton.setEnabled(false);
                            if (copyEmailButton != null) {
                                copyEmailButton.setEnabled(false);
                            }
                            JOptionPane.showMessageDialog(null, "Invalid credentials file. If you want to create a new service account, go back and click \"No\".");
                            return;
                        }
                        gcEmail = object.get("client_email").getAsString();
                        importLabel.setText("<html><div>Imported credentials of <u>" + gcEmail + "</u> service account.</div></html>");
                    } catch (Exception e) {
                        importLabel.setText("Invalid credentials file.");
                        sheetURLField.setEnabled(false);
                        testButton.setEnabled(false);
                        if (copyEmailButton != null) {
                            copyEmailButton.setEnabled(false);
                        }
                        Julti.log(Level.DEBUG, ExceptionUtil.toDetailedString(e));
                        JOptionPane.showMessageDialog(null, "Unknown error while reading the file.");
                        return;
                    }

                    sheetURLField.setEnabled(true);
                    if (copyEmailButton != null) {
                        copyEmailButton.setEnabled(true);
                    }

                    // Store the file path (don't actually copy it until the whole setup is done)
                    credentialsFilePath = file.toPath();

                    /*
                    StatsPluginSettings settings = StatsPluginSettings.getInstance();
                    if (!settings.useSheets) {
                        settings.useSheets = true;
                        StatsPluginSettings.save();
                        Julti.log(Level.INFO, "Updated settings to use Google Sheets.");
                    }

                    Julti.log(Level.INFO, "Imported credentials.json. Trying to connect to Google Sheets.");
                    boolean connected = reloadGoogleSheets();
                    if (!connected) {
                        JOptionPane.showMessageDialog(null, "Imported credentials.json, but did not connect to Google Sheets. Check Julti logs for help.");
                    }*/
                }
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to import credentials.json:\n" + ExceptionUtil.toDetailedString(e));
            }
        };
    }

    private DocumentListener sheetURLFieldLogic(JButton testButton) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                try {
                    String text = e.getDocument().getText(0, e.getDocument().getLength());
                    String sheetId = StatsPluginUtil.extractGoogleSheetsID(text);
                    if (sheetId != null) {
                        googleSheetsId = sheetId;
                    }
                    testButton.setEnabled(sheetId != null);
                } catch (BadLocationException ignored) {
                    testButton.setEnabled(false);
                }
            }
        };
    }

    private ActionListener testButtonLogic() {
        return (a) -> {
            try {
                if (!GoogleSheets.test(googleSheetsId, credentialsFilePath)) {
                    throw new RuntimeException("Provided Google Sheet doesn't have a 'Raw Data' worksheet.");
                }
                JOptionPane.showMessageDialog(null, "Connected to Google Sheets!");
                nextButton.setEnabled(true);
            } catch (Exception e) {
                Julti.log(Level.DEBUG, ExceptionUtil.toDetailedString(e));
                JOptionPane.showMessageDialog(null, "Could not connect to sheet. Verify that: the credentials are correct, your sheet is being shared with the service account, and the sheet is publicly viewable.");
            }
        };
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 5));
        mainPanel.setPreferredSize(new Dimension(610, 355));
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        navigationBar = new JPanel();
        navigationBar.setLayout(new BorderLayout(0, 0));
        navigationBar.setVisible(true);
        mainPanel.add(navigationBar, BorderLayout.SOUTH);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        navigationBar.add(panel1, BorderLayout.WEST);
        exitButton = new JButton();
        exitButton.setText("Exit");
        exitButton.setVisible(false);
        panel1.add(exitButton);
        backButton = new JButton();
        backButton.setEnabled(false);
        backButton.setHideActionText(false);
        backButton.setHorizontalAlignment(0);
        backButton.setText("Back");
        backButton.putClientProperty("html.disable", Boolean.FALSE);
        panel1.add(backButton);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        navigationBar.add(panel2, BorderLayout.EAST);
        nextButton = new JButton();
        nextButton.setEnabled(false);
        nextButton.setText("Next");
        panel2.add(nextButton);
        cardContainer = new JPanel();
        cardContainer.setLayout(new CardLayout(0, 0));
        cardContainer.setMaximumSize(new Dimension(550, 300));
        cardContainer.setMinimumSize(new Dimension(600, 300));
        cardContainer.setPreferredSize(new Dimension(500, 300));
        mainPanel.add(cardContainer, BorderLayout.CENTER);
        card1 = new JPanel();
        card1.setLayout(new BorderLayout(0, 0));
        cardContainer.add(card1, "Card1");
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        card1.add(panel3, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 10));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(panel4, gbc);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setEnabled(true);
        Font label1Font = this.$$$getFont$$$(null, -1, 16, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("<html>Do you want to track stats online through Google Sheets?</html>");
        label1.setVisible(true);
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new BorderLayout(0, 0));
        panel5.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        card1_jep = new JEditorPane();
        card1_jep.setContentType("text/html");
        card1_jep.setEditable(false);
        card1_jep.setEnabled(true);
        card1_jep.setFocusable(false);
        Font card1_jepFont = this.$$$getFont$$$(null, -1, -1, card1_jep.getFont());
        if (card1_jepFont != null) card1_jep.setFont(card1_jepFont);
        card1_jep.setOpaque(false);
        card1_jep.setText("<html>\n<div>- Stats are uploaded to a Google Sheet, which are then available on <a href=\"https://reset-analytics-dev.vercel.app/\">Specnr's Reset Analytics Website</a>. You will be able to see detailed stats on the website which you can easily share with others.</div>\n<div>- Note: If you already use Google Sheets with the \"old\" tracker, still select <i>Yes</i>.</div>\n<br>\n<div><b>If you only want the OBS overlay and you don't care about sharing your stats, select <i>No</i>.<br>NOTE: You currently won't be able to link a Google Sheet if you select No.</b></div>\n</html>\n");
        card1_jep.setVerifyInputWhenFocusTarget(true);
        card1_jep.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        panel6.add(card1_jep, BorderLayout.NORTH);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel4.add(panel7, BorderLayout.CENTER);
        card1_yesButton = new JButton();
        card1_yesButton.setText("Yes");
        panel7.add(card1_yesButton);
        card1_noButton = new JButton();
        card1_noButton.setText("No");
        panel7.add(card1_noButton);
        card2 = new JPanel();
        card2.setLayout(new BorderLayout(0, 0));
        cardContainer.add(card2, "Card2");
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridBagLayout());
        card2.add(panel8, BorderLayout.CENTER);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new BorderLayout(0, 10));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel8.add(panel9, gbc);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 0));
        panel9.add(panel10, BorderLayout.NORTH);
        final JLabel label2 = new JLabel();
        label2.setEnabled(true);
        Font label2Font = this.$$$getFont$$$(null, -1, 16, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText("<html>Have you used the old tracker before?</html>");
        label2.setVisible(true);
        panel10.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new BorderLayout(0, 0));
        panel10.add(panel11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        card2_jep = new JEditorPane();
        card2_jep.setContentType("text/html");
        card2_jep.setEditable(false);
        card2_jep.setEnabled(true);
        card2_jep.setFocusable(false);
        Font card2_jepFont = this.$$$getFont$$$(null, -1, -1, card2_jep.getFont());
        if (card2_jepFont != null) card2_jep.setFont(card2_jepFont);
        card2_jep.setOpaque(false);
        card2_jep.setText("<html>\n<div>- Note: This refers to the widely used <a href=\"https://github.com/Specnr/ResetTracker\">tracker developed by Specnr</a> and further developed by <a href=\"https://github.com/pncakespoon1/ResetTracker/\">pncakespoon</a>.</div>\n<div>- <b>If you don't have a sheet (or if you want a new one), select <i>No</i>.</b></div>\n</html>\n");
        card2_jep.setVerifyInputWhenFocusTarget(true);
        card2_jep.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        panel11.add(card2_jep, BorderLayout.NORTH);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel9.add(panel12, BorderLayout.CENTER);
        card2_yesButton = new JButton();
        card2_yesButton.setText("Yes");
        panel12.add(card2_yesButton);
        card2_noButton = new JButton();
        card2_noButton.setText("No");
        panel12.add(card2_noButton);
        card3 = new JPanel();
        card3.setLayout(new BorderLayout(0, 0));
        cardContainer.add(card3, "Card3");
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridBagLayout());
        card3.add(panel13, BorderLayout.CENTER);
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new BorderLayout(0, 10));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel13.add(panel14, gbc);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 0));
        panel14.add(panel15, BorderLayout.NORTH);
        final JLabel label3 = new JLabel();
        label3.setEnabled(true);
        Font label3Font = this.$$$getFont$$$(null, -1, 16, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("<html>Import existing Google Sheet</html>");
        label3.setVisible(true);
        panel15.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new BorderLayout(0, 0));
        panel15.add(panel16, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JEditorPane editorPane1 = new JEditorPane();
        editorPane1.setContentType("text/html");
        editorPane1.setEditable(false);
        editorPane1.setEnabled(true);
        editorPane1.setFocusable(false);
        Font editorPane1Font = this.$$$getFont$$$(null, -1, 11, editorPane1.getFont());
        if (editorPane1Font != null) editorPane1.setFont(editorPane1Font);
        editorPane1.setOpaque(false);
        editorPane1.setText("<html>\n<div>- Click on import button, then find the old tracker, and select the credentials.json file.</div>\n<div>- Paste the Google Sheets URL below (you can find it in settings.json in the old tracker files)</div>\n<div>- Click \"Test\" so you can be sure it works.</div>\n</html>\n");
        editorPane1.setVerifyInputWhenFocusTarget(true);
        editorPane1.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        panel16.add(editorPane1, BorderLayout.NORTH);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new FormLayout("fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel14.add(panel17, BorderLayout.CENTER);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new BorderLayout(10, 0));
        CellConstraints cc = new CellConstraints();
        panel17.add(panel18, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
        card3_importButton = new JButton();
        card3_importButton.setText("Import credentials.json");
        panel18.add(card3_importButton, BorderLayout.WEST);
        card3_importLabel = new JLabel();
        card3_importLabel.setText("<html>Waiting for import...</html>");
        panel18.add(card3_importLabel, BorderLayout.CENTER);
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel17.add(panel19, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.TOP));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new BorderLayout(0, 5));
        panel20.setEnabled(true);
        panel19.add(panel20);
        final JLabel label4 = new JLabel();
        label4.setText("Google Sheets URL:");
        panel20.add(label4, BorderLayout.NORTH);
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new BorderLayout(10, 0));
        panel20.add(panel21, BorderLayout.CENTER);
        card3_sheetURLField = new JTextField();
        card3_sheetURLField.setEnabled(false);
        card3_sheetURLField.setPreferredSize(new Dimension(500, 30));
        panel21.add(card3_sheetURLField, BorderLayout.CENTER);
        card3_testButton = new JButton();
        card3_testButton.setEnabled(false);
        card3_testButton.setText("Test");
        panel21.add(card3_testButton, BorderLayout.EAST);
        card4 = new JPanel();
        card4.setLayout(new BorderLayout(0, 0));
        cardContainer.add(card4, "Card4");
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridBagLayout());
        card4.add(panel22, BorderLayout.CENTER);
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new BorderLayout(0, 10));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel22.add(panel23, gbc);
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 0));
        panel23.add(panel24, BorderLayout.NORTH);
        final JLabel label5 = new JLabel();
        label5.setEnabled(true);
        Font label5Font = this.$$$getFont$$$(null, -1, 16, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setText("<html>Create and link a Google Sheet</html>");
        label5.setVisible(true);
        panel24.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new BorderLayout(0, 0));
        panel24.add(panel25, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        card4_jep = new JEditorPane();
        card4_jep.setContentType("text/html");
        card4_jep.setEditable(false);
        card4_jep.setEnabled(true);
        card4_jep.setFocusable(false);
        Font card4_jepFont = this.$$$getFont$$$(null, -1, 11, card4_jep.getFont());
        if (card4_jepFont != null) card4_jep.setFont(card4_jepFont);
        card4_jep.setOpaque(false);
        card4_jep.setText("<html>\n<div>1. Create a Google Cloud Service Account by following <a href=\"https://docs.google.com/document/d/e/2PACX-1vSiq2rHiZp9CcACOGg1EHf0nckk28FlCNARLYmhjiTL_O5x2PPA6UOHxX_mWCPj1hTwdxMzUmCtFDzL/pub\">this tutorial</a>.</div>\n<div>2. Import the credentials file that you downloaded at the end of that tutorial.</div>\n<div>3. Create the Google Sheet, and add the GC Service account to it by following <a href=\"https://docs.google.com/document/d/e/2PACX-1vQWZp6dutujZfG-RdqaPYwCZuN8FZg8WMIjt_-308BMOUXznBtWM4GHgjc6khE__lU4SveD3uQrXgkU/pub\">this tutorial</a>.</div>\n<div>4. Paste the link to your Google Sheet at the bottom of this page, then click 'Test'.</div>\n</html>\n");
        card4_jep.setVerifyInputWhenFocusTarget(true);
        card4_jep.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        panel25.add(card4_jep, BorderLayout.NORTH);
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new FormLayout("fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel23.add(panel26, BorderLayout.CENTER);
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new BorderLayout(10, 5));
        panel26.add(panel27, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
        card4_importButton = new JButton();
        card4_importButton.setText("Import credentials.json");
        panel27.add(card4_importButton, BorderLayout.WEST);
        card4_importLabel = new JLabel();
        card4_importLabel.setText("<html>Waiting for import...</html>");
        panel27.add(card4_importLabel, BorderLayout.CENTER);
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new BorderLayout(0, 0));
        panel27.add(panel28, BorderLayout.SOUTH);
        card4_copyEmailButton = new JButton();
        card4_copyEmailButton.setEnabled(false);
        card4_copyEmailButton.setText("Copy Service Account Email");
        panel28.add(card4_copyEmailButton, BorderLayout.WEST);
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel26.add(panel29, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.TOP));
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new BorderLayout(0, 5));
        panel30.setEnabled(true);
        panel29.add(panel30);
        final JLabel label6 = new JLabel();
        label6.setText("Google Sheets URL:");
        panel30.add(label6, BorderLayout.NORTH);
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new BorderLayout(10, 0));
        panel30.add(panel31, BorderLayout.CENTER);
        card4_sheetURLField = new JTextField();
        card4_sheetURLField.setEnabled(false);
        card4_sheetURLField.setPreferredSize(new Dimension(500, 30));
        panel31.add(card4_sheetURLField, BorderLayout.CENTER);
        card4_testButton = new JButton();
        card4_testButton.setEnabled(false);
        card4_testButton.setText("Test");
        panel31.add(card4_testButton, BorderLayout.EAST);
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new CardLayout(0, 0));
        cardContainer.add(panel32, "Card5");
        final JPanel panel33 = new JPanel();
        panel33.setLayout(new GridBagLayout());
        panel32.add(panel33, "Card1");
        final JPanel panel34 = new JPanel();
        panel34.setLayout(new BorderLayout(0, 10));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel33.add(panel34, gbc);
        final JPanel panel35 = new JPanel();
        panel35.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, 0));
        panel34.add(panel35, BorderLayout.NORTH);
        final JLabel label7 = new JLabel();
        label7.setEnabled(true);
        Font label7Font = this.$$$getFont$$$(null, -1, 16, label7.getFont());
        if (label7Font != null) label7.setFont(label7Font);
        label7.setText("<html>Setup completed!</html>");
        label7.setVisible(true);
        panel35.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel36 = new JPanel();
        panel36.setLayout(new BorderLayout(0, 0));
        panel35.add(panel36, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        card5_jep = new JEditorPane();
        card5_jep.setContentType("text/html");
        card5_jep.setEditable(false);
        card5_jep.setEnabled(true);
        card5_jep.setFocusable(false);
        Font card5_jepFont = this.$$$getFont$$$(null, -1, 11, card5_jep.getFont());
        if (card5_jepFont != null) card5_jep.setFont(card5_jepFont);
        card5_jep.setOpaque(false);
        card5_jep.setText("<html>\n<div>You can now configure the OBS overlay and change the tracker settings. For any questions, check the <a href=\"https://github.com/marin774/Julti-Stats-Plugin\">Github</a> or contact me on Discord @marin774.</div>\n</html>\n");
        card5_jep.setVerifyInputWhenFocusTarget(true);
        card5_jep.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        panel36.add(card5_jep, BorderLayout.NORTH);
        final JPanel panel37 = new JPanel();
        panel37.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow"));
        panel34.add(panel37, BorderLayout.CENTER);
        finishButton = new JButton();
        finishButton.setText("Finish");
        panel37.add(finishButton, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
