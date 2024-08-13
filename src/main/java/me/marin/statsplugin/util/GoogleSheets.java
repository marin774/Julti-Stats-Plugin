package me.marin.statsplugin.util;

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
import me.marin.statsplugin.io.StatsFileIO;
import me.marin.statsplugin.io.StatsPluginSettings;
import me.marin.statsplugin.stats.StatsRecord;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleSheets {

    private static final String APPLICATION_NAME = "Julti Stats Plugin";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private final Path credentialsPath;
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
            GoogleCredentials credential = authorize(credentialsPath);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);
            this.service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            getRawDataSheet();

            Julti.log(Level.INFO, "Connected to Google Sheets!");
            setHeaderColumns();
            tempFix();
            return true;
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to connect to Google Sheets: " + ExceptionUtil.toDetailedString(e));
        }
        return false;
    }

    private Sheet rawDataSheet;

    private void getRawDataSheet() throws IOException {
        if (rawDataSheet == null) {
            List<DataFilter> dataFilters = new ArrayList<>();
            dataFilters.add(new DataFilter().setA1Range("Raw Data"));
            this.rawDataSheet = service.spreadsheets()
                    .getByDataFilter(spreadsheetId, new GetSpreadsheetByDataFilterRequest().setIncludeGridData(false).setDataFilters(dataFilters))
                    .execute()
                    .getSheets()
                    .get(0);
        }
    }

    /**
     * @deprecated Temporary fix for a bug in older versions, will be removed in a later version
     */
    private void tempFix() {
        StatsPluginUtil.runAsync("google-sheets", () -> {
            try {
                getRawDataSheet();
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

    private void setHeaderColumns() {
        StatsPluginUtil.runAsync("google-sheets", () -> {
            try {
                getRawDataSheet();
                Integer sheetId = rawDataSheet.getProperties().getSheetId();

                String[] headerLabels = new String[]{
                        "Date and Time", "Iron Source", "Enter Type", "Gold Source", "Spawn Biome", "RTA", "Wood",
                        "Iron Pickaxe", "Nether", "Bastion", "Fortress", "Nether Exit", "Stronghold", "End", "Retimed IGT",
                        "IGT", "Gold Dropped", "Blaze Rods", "Blazes", "", "", "", "", "", "", "Iron",
                        "Wall Resets Since Prev",
                        "Played Since Prev", "RTA Since Prev", "Break RTA Since Prev", "Wall Time Since Prev",
                        "Session Marker",
                        "RTA Distribution", "seed", "Diamond Pick", "Pearls Thrown", "Deaths",
                        "Obsidian Placed", "Diamond Sword", "Blocks Mined"
                };
                RowData rowData = new RowData();
                List<CellData> cellData = new ArrayList<>();
                for (String headerLabel : headerLabels) {
                    cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(headerLabel)));
                }
                rowData.setValues(cellData);

                BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
                List<Request> requests = new ArrayList<>();
                requests.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                        .setProperties(new SheetProperties().setSheetId(sheetId)
                                .setGridProperties(new GridProperties().setColumnCount(headerLabels.length)))
                        .setFields("gridProperties.columnCount")));
                requests.add(new Request().setUpdateCells(
                        new UpdateCellsRequest()
                                .setRows(Collections.singletonList(rowData))
                                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(0))
                                .setFields("userEnteredValue")
                ));
                batchUpdateRequest.setRequests(requests);
                service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            } catch (Exception e) {
                Julti.log(Level.ERROR, "Failed to update Google Sheets: " + ExceptionUtil.toDetailedString(e));
            }
        });
    }

    public void insertRecord(StatsRecord record) {
        StatsPluginUtil.runAsync("google-sheets", () -> {
            try {
                //if (StatsPluginSettings.getInstance().simulateNoInternet) throw new RuntimeException("simulating offline");

                getRawDataSheet();
                Integer sheetId = rawDataSheet.getProperties().getSheetId();

                List<Request> requests = new ArrayList<>();

                List<StatsRecord> records = new ArrayList<>();
                records.addAll(StatsFileIO.getInstance().getAllTempStats());
                records.add(record);

                requests.add(new Request().setInsertDimension(
                                new InsertDimensionRequest()
                                        .setInheritFromBefore(false) /* inherit from the row after instead */
                                        .setRange(
                                                new DimensionRange()
                                                        .setDimension("ROWS")
                                                        .setSheetId(sheetId)
                                                        .setStartIndex(1)
                                                        .setEndIndex(1 + records.size())
                                        )
                        )
                );

                List<RowData> rowDataList = new ArrayList<>();
                for (int i = records.size() - 1; i >= 0; i--) {
                    StatsRecord statsRecord = records.get(i);
                    rowDataList.add(statsRecord.getGoogleSheetsRowData());
                }

                requests.add(new Request().setUpdateCells(
                                new UpdateCellsRequest()
                                        .setRows(rowDataList)
                                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(1).setEndRowIndex(1 + records.size()).setStartColumnIndex(0))
                                        .setFields("userEnteredValue")
                        )
                );

                service.spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();

                StatsFileIO.getInstance().clearTempStats();
            } catch (Exception e) {
                // Failed to update Google Sheets, either no internet, or GSheets is unavailable
                // Save the record to a temp file, and then upload all runs from temp.csv on next run
                StatsFileIO.getInstance().writeTempStats(record);

                if (!(e instanceof SocketTimeoutException)) {
                    Julti.log(Level.ERROR, "Failed to update Google Sheets (run was saved locally): " + ExceptionUtil.toDetailedString(e));
                }
            }
        });

    }

    private static GoogleCredentials authorize(Path credentialsPath) throws IOException {
        return ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath.toFile())).createScoped(SCOPES);
    }

    public static boolean test(String spreadsheetId, Path credentialsPath) throws IOException, GeneralSecurityException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credential = authorize(credentialsPath);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();

        List<DataFilter> dataFilters = new ArrayList<>();
        dataFilters.add(new DataFilter().setA1Range("Raw Data"));
        return service.spreadsheets()
                .getByDataFilter(spreadsheetId, new GetSpreadsheetByDataFilterRequest().setIncludeGridData(false).setDataFilters(dataFilters))
                .execute()
                .getSheets()
                .size() == 1;
    }

}
