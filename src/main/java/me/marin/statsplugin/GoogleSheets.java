package me.marin.statsplugin;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import me.marin.statsplugin.io.StatsPluginSettings;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class GoogleSheets {

    private static final String APPLICATION_NAME = "Julti Stats Plugin";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private Path credentialsPath;
    private Sheets service;
    private String spreadsheetId;

    public GoogleSheets(Path credentials) {
        this.credentialsPath = credentials;
    }

    public boolean isConnected() {
        return this.service != null;
    }

    public boolean connect() {
        try {
            NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            String sheetLink = StatsPluginSettings.getInstance().sheetLink;
            if (sheetLink == null) {
                Julti.log(Level.ERROR, "Couldn't find Google Sheets link in settings.json. Update settings and click 'Reconnect to Google Sheets' button.");
                return false;
            }
            this.spreadsheetId = StatsPluginUtil.extractGoogleSheetsID(sheetLink);
            if (this.spreadsheetId == null) {
                Julti.log(Level.ERROR, "Couldn't find Google Sheets ID in URL (" + sheetLink + "). Make sure that the provided link is valid, then click 'Reconnect to Google Sheets' button.");
                return false;
            }
            GoogleCredentials credential = authorize();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);
            this.service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Julti.log(Level.INFO, "Connected to Google Sheets!");
            StatsPluginUtil.runAsync("google-sheets", this::tempFix);
            return true;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to connect to Google Sheets: " + ExceptionUtil.toDetailedString(e));
        }
        return false;
    }

    private Sheet rawDataSheet;

    private void updateRawDataSheet() throws IOException {
        if (rawDataSheet == null) {
            this.rawDataSheet = service.spreadsheets().getByDataFilter(spreadsheetId, new GetSpreadsheetByDataFilterRequest().setIncludeGridData(false).setDataFilters(
                    List.of(new DataFilter().setA1Range("Raw Data"))
            )).execute().getSheets().get(0);
        }
    }

    /**
     * @deprecated Temporary fix for a bug in older versions, will be removed in a later version
     */
    private void tempFix() {
        StatsPluginUtil.runAsync("google-sheets", () -> {
            try {
                updateRawDataSheet();
                Integer sheetId = rawDataSheet.getProperties().getSheetId();

                List<Request> requests = new ArrayList<>();
                requests.add(new Request().setFindReplace(
                        new FindReplaceRequest()
                                .setFind("Buried Treasure w/ TNT")
                                .setMatchCase(true)
                                .setReplacement("Buried Treasure w/ tnt")
                                .setSheetId(sheetId)
                ));
                service.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();

            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to update Google Sheets: " + ExceptionUtil.toDetailedString(e));
            }
        });
    }

    public void insertRecord(StatsRecord record) {
        StatsPluginUtil.runAsync("google-sheets", () -> {
            try {
                updateRawDataSheet();
                Integer sheetId = rawDataSheet.getProperties().getSheetId();

                List<Request> requests = new ArrayList<>();
                requests.add(new Request().setInsertDimension(
                                new InsertDimensionRequest()
                                        .setInheritFromBefore(false) /* inherit from the row after! */
                                        .setRange(
                                                new DimensionRange()
                                                        .setDimension("ROWS")
                                                        .setSheetId(sheetId)
                                                        .setStartIndex(1)
                                                        .setEndIndex(2)
                                        )
                        )
                );

                requests.add(new Request().setUpdateCells(
                                new UpdateCellsRequest()
                                        .setRows(Collections.singletonList(record.getGoogleSheetsRowData()))
                                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(1).setEndRowIndex(2).setStartColumnIndex(0))
                                        .setFields("userEnteredValue")
                        )
                );

                service.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();

            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to update Google Sheets: " + ExceptionUtil.toDetailedString(e));
            }
        });

    }

    private GoogleCredentials authorize() throws IOException {
        return ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath.toFile())).createScoped(SCOPES);
    }

}
