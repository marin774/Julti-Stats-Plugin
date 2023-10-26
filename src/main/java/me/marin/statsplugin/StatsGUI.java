package me.marin.statsplugin;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import me.marin.statsplugin.io.OldRecordBopperRunnable;
import me.marin.statsplugin.io.StatsFileIO;
import me.marin.statsplugin.io.StatsPluginSettings;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.util.ExceptionUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static me.marin.statsplugin.StatsPlugin.*;

public class StatsGUI extends JFrame {

    private boolean isClosed = false;

    private JPanel mainPanel;
    private JCheckBox trackerEnabledCheckbox;
    private JButton importCredentialsJsonButton;
    private JButton importSettingsJsonButton;
    private JButton importStatsCsvButton;
    private JButton openSettingsJson;
    private JButton openStatsCsv;
    private JButton reconnectToGoogleSheetsButton;
    private JButton openGoogleSheetButton;
    private JButton openStatsBrowserButton;
    private JButton reloadSettingsButton;
    private JButton clearSpeedrunIGTRecordsButton;

    public StatsGUI() {
        this.setContentPane(mainPanel);
        this.setTitle("Stats Plugin");
        this.pack();
        this.setVisible(true);
        this.setLocation(JultiGUI.getJultiGUI().getLocation());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isClosed = true;
            }
        });

        trackerEnabledCheckbox.setSelected(StatsPluginSettings.getInstance().trackerEnabled);

        trackerEnabledCheckbox.addActionListener(e -> {
            StatsPluginSettings settings = StatsPluginSettings.getInstance();
            settings.trackerEnabled = trackerEnabledCheckbox.isSelected();
            StatsPluginSettings.save();
            Julti.log(Level.INFO, settings.trackerEnabled ? "Now tracking stats." : "No longer tracking stats.");
        });

        importCredentialsJsonButton.addActionListener(a -> {
            try {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(".json", "json");
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter(".json", "json");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    Files.copy(file.toPath(), STATS_SETTINGS_PATH, StandardCopyOption.REPLACE_EXISTING);

                    StatsPluginSettings.load();

                    JOptionPane.showMessageDialog(null, "Imported settings.");
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
                FileNameExtensionFilter filter = new FileNameExtensionFilter(".csv", "csv");
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

        openSettingsJson.addActionListener(a -> {
            try {
                Desktop.getDesktop().open(STATS_SETTINGS_PATH.toFile());
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to open settings.json:\n" + ExceptionUtil.toDetailedString(e));
            }
        });

        openStatsCsv.addActionListener(a -> {
            try {
                Desktop.getDesktop().open(StatsFileIO.STATS_CSV_PATH.toFile());
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to open stats.csv:\n" + ExceptionUtil.toDetailedString(e));
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
            Julti.log(Level.INFO, "Reloaded settings.");
            JOptionPane.showMessageDialog(null, "Reloaded settings.");
        });

        clearSpeedrunIGTRecordsButton.addActionListener(a -> {
            new Thread(new OldRecordBopperRunnable(), "old-record-bopper").start();
            JOptionPane.showMessageDialog(null, "Clearing records, check Julti logs for more information.");
        });

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
        mainPanel.setPreferredSize(new Dimension(600, 450));
        mainPanel.setRequestFocusEnabled(true);
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        trackerEnabledCheckbox = new JCheckBox();
        trackerEnabledCheckbox.setText("Tracker enabled?");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        mainPanel.add(trackerEnabledCheckbox, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(panel1, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(5, 0, 5, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel1.add(panel2, gbc);
        importSettingsJsonButton = new JButton();
        importSettingsJsonButton.setText("Import settings.json");
        panel2.add(importSettingsJsonButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importCredentialsJsonButton = new JButton();
        importCredentialsJsonButton.setText("Import credentials.json");
        panel2.add(importCredentialsJsonButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importStatsCsvButton = new JButton();
        importStatsCsvButton.setText("Import stats.csv");
        panel2.add(importStatsCsvButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Import:");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 4, new Insets(5, 0, 5, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(panel3, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Open:");
        panel3.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openGoogleSheetButton = new JButton();
        openGoogleSheetButton.setText("Open Google Sheet");
        panel3.add(openGoogleSheetButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openStatsBrowserButton = new JButton();
        openStatsBrowserButton.setText("Open Stats (browser)");
        panel3.add(openStatsBrowserButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openSettingsJson = new JButton();
        openSettingsJson.setActionCommand("Open settings.json");
        openSettingsJson.setLabel("Open settings.json");
        openSettingsJson.setText("Open settings.json");
        panel3.add(openSettingsJson, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openStatsCsv = new JButton();
        openStatsCsv.setActionCommand("Open stats.csv");
        openStatsCsv.setLabel("Open stats.csv");
        openStatsCsv.setText("Open stats.csv");
        panel3.add(openStatsCsv, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.VERTICAL;
        panel1.add(panel4, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("Utility:");
        panel4.add(label3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reconnectToGoogleSheetsButton = new JButton();
        reconnectToGoogleSheetsButton.setText("Reconnect to Google Sheets");
        panel4.add(reconnectToGoogleSheetsButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadSettingsButton = new JButton();
        reloadSettingsButton.setText("Reload settings");
        panel4.add(reloadSettingsButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 0, 10, 0);
        mainPanel.add(separator1, gbc);
        final JSeparator separator2 = new JSeparator();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 0, 10, 0);
        mainPanel.add(separator2, gbc);
        final JSeparator separator3 = new JSeparator();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 0, 10, 0);
        mainPanel.add(separator3, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
