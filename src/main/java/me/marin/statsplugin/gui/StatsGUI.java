package me.marin.statsplugin.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import me.marin.statsplugin.StatsPluginUtil;
import me.marin.statsplugin.io.OldRecordBopperRunnable;
import me.marin.statsplugin.io.StatsPluginSettings;
import me.marin.statsplugin.stats.Session;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.util.ExceptionUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;

import static me.marin.statsplugin.StatsPlugin.*;

public class StatsGUI extends JFrame {

    private boolean isClosed = false;

    private JPanel mainPanel;
    private JCheckBox trackerEnabledCheckbox;
    private JButton setupButton;
    private JButton openSettingsJson;
    private JButton reconnectToGoogleSheetsButton;
    private JButton openGoogleSheetButton;
    private JButton openStatsBrowserButton;
    private JButton reloadSettingsButton;
    private JButton clearSpeedrunIGTRecordsButton;
    private JButton OBSOverlayButton;
    private JButton startANewSessionButton;
    private JPanel setupPanel;
    private JPanel settingsPanel;
    private JCheckBox simulateOfflineCheckbox;

    private OBSOverlayGUI obsOverlayGUI;
    private SetupGUI setupGUI;

    public StatsGUI() {
        this.setContentPane(mainPanel);
        this.setTitle("Stats Plugin Config");
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

        updateGUI();

        trackerEnabledCheckbox.addActionListener(e -> {
            StatsPluginSettings settings = StatsPluginSettings.getInstance();
            settings.trackerEnabled = trackerEnabledCheckbox.isSelected();
            StatsPluginSettings.save();
            Julti.log(Level.INFO, settings.trackerEnabled ? "Now tracking stats." : "No longer tracking stats.");
        });

        // This is used for testing Google Sheets as if you or Google Sheets are down
        simulateOfflineCheckbox.addActionListener(e -> {
            StatsPluginSettings settings = StatsPluginSettings.getInstance();
            settings.simulateNoInternet = simulateOfflineCheckbox.isSelected();
            StatsPluginSettings.save();
        });

        /*
        importCredentialsJsonButton.addActionListener(a -> {
            try {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*.json", "json");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    Files.copy(file.toPath(), GOOGLE_SHEETS_CREDENTIALS_PATH, StandardCopyOption.REPLACE_EXISTING);

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
                    }
                }
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to import credentials.json:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        importSettingsJsonButton.addActionListener(a -> {
            try {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*.json", "json");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    Files.copy(file.toPath(), STATS_SETTINGS_PATH, StandardCopyOption.REPLACE_EXISTING);

                    boolean wasEnabled = StatsPluginSettings.getInstance().trackerEnabled;

                    StatsPluginSettings.load();
                    updateGUI();

                    boolean isEnabled = StatsPluginSettings.getInstance().trackerEnabled;

                    JOptionPane.showMessageDialog(null, "Imported settings." + ((wasEnabled ^ isEnabled) ? "NOTE: Tracker is now " + (isEnabled ? "enabled." : "disabled.") : ""));
                    Julti.log(Level.INFO, "Imported & reloaded settings.");
                }
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to import settings.json:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        importStatsCsvButton.addActionListener(a -> {
            try {
                int option = JOptionPane.showOptionDialog(
                        null,
                        "This will overwrite existing Julti stats. Are you sure you want to import stats?",
                        "Warning",
                        YES_NO_OPTION,
                        WARNING_MESSAGE,
                        null,
                        null,
                        "Yes");

                if (option != 0) {
                    return;
                }

                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*.csv", "csv");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    Files.copy(file.toPath(), StatsFileIO.STATS_CSV_PATH, StandardCopyOption.REPLACE_EXISTING);

                    StatsPluginSettings.load();
                    StatsPluginSettings.save();

                    JOptionPane.showMessageDialog(null, "Imported local stats.");
                    Julti.log(Level.INFO, "Imported local stats.");
                }

            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to import stats.csv:\n" + ExceptionUtil.toDetailedString(e));
            }
        });
        */
        setupButton.addActionListener(a -> {
            if (setupGUI == null || setupGUI.isClosed()) {
                setupGUI = new SetupGUI();
            } else {
                setupGUI.requestFocus();
            }
        });


        openSettingsJson.addActionListener(a -> {
            try {
                Desktop.getDesktop().open(STATS_SETTINGS_PATH.toFile());
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to open settings.json:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        openGoogleSheetButton.addActionListener(a -> {
            try {
                String sheetLink = StatsPluginSettings.getInstance().sheetLink;
                if (sheetLink == null) {
                    JOptionPane.showMessageDialog(null, "You haven't set up a Google Sheet link in settings.json!");
                    return;
                }
                Desktop.getDesktop().browse(URI.create(sheetLink));
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to open sheets:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        openStatsBrowserButton.addActionListener(a -> {
            try {
                String sheetLink = StatsPluginSettings.getInstance().sheetLink;
                if (sheetLink == null) {
                    JOptionPane.showMessageDialog(null, "You haven't set up a Google Sheet link in settings.json!");
                    return;
                }
                String sheetsID = StatsPluginUtil.extractGoogleSheetsID(sheetLink);
                Desktop.getDesktop().browse(URI.create("https://reset-analytics-dev.vercel.app/sheet/" + sheetsID));
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to open sheets:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        reconnectToGoogleSheetsButton.addActionListener(a -> {
            boolean connected = reloadGoogleSheets();
            if (!connected) {
                JOptionPane.showMessageDialog(null, "Could not connect to Google Sheets. Check Julti logs for help.");
            } else {
                JOptionPane.showMessageDialog(null, "Connected to Google Sheets!");
            }
        });

        reloadSettingsButton.addActionListener(a -> {
            StatsPluginSettings.load();
            updateGUI();
            Julti.log(Level.INFO, "Reloaded settings.");
            JOptionPane.showMessageDialog(null, "Reloaded settings.");
        });

        clearSpeedrunIGTRecordsButton.addActionListener(a -> {
            new Thread(new OldRecordBopperRunnable(), "old-record-bopper").start();
            JOptionPane.showMessageDialog(null, "Clearing records, check Julti logs for more information.");
        });

        OBSOverlayButton.addActionListener(a -> {
            if (obsOverlayGUI == null || obsOverlayGUI.isClosed()) {
                obsOverlayGUI = new OBSOverlayGUI();
            } else {
                obsOverlayGUI.requestFocus();
            }
        });

        startANewSessionButton.addActionListener(a -> {
            CURRENT_SESSION = new Session();
            Julti.log(Level.INFO, "Started a new session!");
            JOptionPane.showMessageDialog(null, "New session will begin with your next run.");
        });

    }

    public void updateGUI() {
        trackerEnabledCheckbox.setSelected(StatsPluginSettings.getInstance().trackerEnabled);

        settingsPanel.setVisible(StatsPluginSettings.getInstance().completedSetup);
        setupPanel.setVisible(!StatsPluginSettings.getInstance().completedSetup);

        openStatsBrowserButton.setVisible(StatsPluginSettings.getInstance().useSheets);
        openGoogleSheetButton.setVisible(StatsPluginSettings.getInstance().useSheets);
        reconnectToGoogleSheetsButton.setVisible(StatsPluginSettings.getInstance().useSheets);
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setPreferredSize(new Dimension(600, 350));
        mainPanel.setRequestFocusEnabled(true);
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(panel1, gbc);
        setupPanel = new JPanel();
        setupPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel1.add(setupPanel, gbc);
        setupButton = new JButton();
        setupButton.setText("Setup Tracker");
        setupPanel.add(setupButton);
        final JLabel label1 = new JLabel();
        label1.setText("Complete the quick Setup so you can start using the tracker.");
        setupPanel.add(label1);
        settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPanel.setVisible(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(settingsPanel, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(5, 0, 5, 0), -1, -1));
        settingsPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openGoogleSheetButton = new JButton();
        openGoogleSheetButton.setText("Open Google Sheet");
        openGoogleSheetButton.setVisible(true);
        panel2.add(openGoogleSheetButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openStatsBrowserButton = new JButton();
        openStatsBrowserButton.setText("View Stats in browser");
        openStatsBrowserButton.setVisible(true);
        panel2.add(openStatsBrowserButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        OBSOverlayButton = new JButton();
        OBSOverlayButton.setText("Configure OBS overlay");
        panel2.add(OBSOverlayButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearSpeedrunIGTRecordsButton = new JButton();
        clearSpeedrunIGTRecordsButton.setText("Clear SpeedrunIGT records");
        panel2.add(clearSpeedrunIGTRecordsButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Utility:");
        panel2.add(label2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        settingsPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Debug:");
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startANewSessionButton = new JButton();
        startANewSessionButton.setText("Start a new session");
        panel3.add(startANewSessionButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reconnectToGoogleSheetsButton = new JButton();
        reconnectToGoogleSheetsButton.setText("Reconnect to Google Sheets");
        panel3.add(reconnectToGoogleSheetsButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadSettingsButton = new JButton();
        reloadSettingsButton.setText("Reload settings");
        panel3.add(reloadSettingsButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(5, 0, 5, 0), -1, -1));
        settingsPanel.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Settings:");
        panel4.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openSettingsJson = new JButton();
        openSettingsJson.setActionCommand("Open settings.json");
        openSettingsJson.setLabel("Edit file manually");
        openSettingsJson.setText("Edit file manually");
        panel5.add(openSettingsJson, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        trackerEnabledCheckbox = new JCheckBox();
        trackerEnabledCheckbox.setText("Enable tracker?");
        trackerEnabledCheckbox.setVisible(true);
        panel5.add(trackerEnabledCheckbox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        simulateOfflineCheckbox = new JCheckBox();
        simulateOfflineCheckbox.setText("Simulate offline?");
        simulateOfflineCheckbox.setVisible(false);
        panel5.add(simulateOfflineCheckbox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
