package datapreparer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import core.GlobalConfigs;
import core.GlobalConfigs.*;
import static core.GlobalConfigs.TRAINING_TYPES_SIZE;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of double arrays
 */
public class TrainingDataGenerator {

    static String RESOURCE_PATH = GlobalConfigs.DEFAULT_PATH + "resources\\";
    static ConcurrentHashMap<String, ArrayList> _trainingDataMap = new ConcurrentHashMap();

    public void generateTrainingData(boolean writeToFile, boolean writeToMomory, boolean normalize) {
        ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
        System.out.println("Starting to generate training data.");
        //The code currently being processed
        String currentCode = "empty";
        for (String code : instruments) {
            currentCode = code;
            //Get raw data organized by date for each instrument
            ConcurrentHashMap<String, Object[]> raw_data_map = loadRawDataFromFile(code);
            ArrayList<Object[]> temp_data = new ArrayList();
            //Start data processing
            for (String date : raw_data_map.keySet()) {

                Object[] row = new Object[TRAINING_TYPES_SIZE];
                processToTrainingItems(date, raw_data_map, row);
                if (hasNull(row)) {
                    continue;
                }
                temp_data.add(row);
            }

            //Data processing end, start to save results.
            if (writeToMomory) {
                _trainingDataMap.put(code, temp_data);
            }
            if (writeToFile) {
                File training_file = new File(RESOURCE_PATH + code + "//" + code + "_Training.csv");
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(training_file, false)))) {
                    //  writer.println("\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\",\"12\",\"13\",\"14\",\"15\"");
                    for (Object[] row : temp_data) {
                        String temp = "";
                        for (Object item : row) {
                            temp += (item + ",");
                        }
                        //remove last comma and write to file
                        writer.println(temp.substring(0, temp.length() - 1));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.SEVERE, "Failed to write to training file for: " + code, ex);
                }
                if (normalize) {
//                    File normalized_file = new File(RESOURCE_PATH + code + "//" + code + "_Training_Normalized.csv");
//                    EncogAnalyst analyst = new EncogAnalyst();
//                    //CmdReset reset=new CmdReset(analyst);
//                    //reset.executeCommand(null);
//                    AnalystWizard wizard = new AnalystWizard(analyst);
//                    
////                    wizard.setTargetField("13");
////                    wizard.setTargetField("14");
////                    wizard.setTargetField("15");
//                    wizard.wizard(training_file, true, AnalystFileFormat.DECPNT_COMMA);
//                    
//                    System.out.println(analyst.getScript().getProperties());
//                    for (AnalystField field : analyst.getScript().getNormalize().getNormalizedFields()) {
//                        StringBuilder line = new StringBuilder();
//                        line.append(field.getName());
//                        line.append(",action=");
//                        line.append(field.getAction());
//                        line.append(",min=");
//                        line.append(field.getActualLow());
//                        line.append(",max=");
//                        line.append(field.getActualHigh());
//                        System.out.println(line.toString());
//                    }
//                    final AnalystNormalizeCSV norm = new AnalystNormalizeCSV();
//                    norm.analyze(training_file, true, CSVFormat.ENGLISH, analyst);
//                    norm.setProduceOutputHeaders(true);
//                    norm.normalize(normalized_file);
//                    Encog.getInstance().shutdown();
                }
            }
        }
        System.out.println("Training data successfully generated.");
    }

    // Return a map of raw data array
    // Data order(index start at 0!): 1, open; 2, high; 3, low; 4, close; 5, volume
    // TODO: import data from different files to one map
    private ConcurrentHashMap<String, Object[]> loadRawDataFromFile(String code) {
        ConcurrentHashMap<String, Object[]> raw_data_map = new ConcurrentHashMap();

        File folder = new File(RESOURCE_PATH + code);
        if (!folder.isDirectory()) {
            Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.WARNING, "Failed to find directory for: {0}", code);
            return null;
        }
        File file = new File(RESOURCE_PATH + code + "//" + code + ".csv");
        if (file.isFile()) {
            try (BufferedReader reader = new BufferedReader(
                    new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    // Data order: 1, open; 2, high; 3, low; 4, close; 5, volume
                    Object[] data = new Object[]{Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5])};
                    raw_data_map.put(line.substring(0, 10), data);
                }
            } catch (Exception ex) {
                Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.SEVERE, "Error when loading csv file for raw data.", ex);
            }
        } else {
            Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.WARNING, "Data file for {0} does not exist.", code);
            return null;
        }
        return raw_data_map;
    }

    // storageRow will store new items created
    private void processToTrainingItems(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        //Input data
        addRawTrends(date, rawDataMap, storageRow);
        addVelocities(date, rawDataMap, storageRow);
        addSeasonOfYear(date, storageRow, TRAINIG_TYPES.SEASON_YEAR.index());

        //Output data
        addCalssValues(date, rawDataMap, storageRow);

    }

    private void addCalssValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addRawTrend(date, rawDataMap, storageRow, -7, 0, TRAINIG_TYPES.FUTTREND_7d.index());
        addRawTrend(date, rawDataMap, storageRow, -14, 0, TRAINIG_TYPES.FUTTREND_14d.index());
        addRawTrend(date, rawDataMap, storageRow, -24, 0, TRAINIG_TYPES.FUTTREND_28d.index());
        addRawTrend(date, rawDataMap, storageRow, -49, 0, TRAINIG_TYPES.FUTTREND_49d.index());

        addSituationClass(date, rawDataMap, storageRow, -7, 0, TRAINIG_TYPES.FUTSITU_7d.index(), 0.03);
    }

    private void addRawTrends(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addRawTrend(date, rawDataMap, storageRow, 0, 0, TRAINIG_TYPES.RAWTREND_0d.index());
        addRawTrend(date, rawDataMap, storageRow, 1, 0, TRAINIG_TYPES.RAWTREND_1d.index());
        addRawTrend(date, rawDataMap, storageRow, 3, 3, TRAINIG_TYPES.RAWTREND_3d.index());
        addRawTrend(date, rawDataMap, storageRow, 7, 7, TRAINIG_TYPES.RAWTREND_1w.index());
        addRawTrend(date, rawDataMap, storageRow, 14, 14, TRAINIG_TYPES.RAWTREND_2w.index());
        addRawTrend(date, rawDataMap, storageRow, 28, 28, TRAINIG_TYPES.RAWTREND_4w.index());
        addRawTrend(date, rawDataMap, storageRow, 49, 49, TRAINIG_TYPES.RAWTREND_7w.index());
        addRawTrend(date, rawDataMap, storageRow, 63, 63, TRAINIG_TYPES.RAWTREND_9w.index());
        addRawTrend(date, rawDataMap, storageRow, 72, 72, TRAINIG_TYPES.RAWTREND_12w.index());
    }

    private void addVelocities(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        // 21 day (plus weekends) duration NASDAQ velocity
        addVelocity(date, rawDataMap, storageRow, 30, TRAINIG_TYPES.VELOCITY_0d.index());
    }

    private void addSeasonOfYear(String date, Object[] storageRow, int storageIndex) {
        storageRow[storageIndex] = StatCalculator.CalculateSeasonOfYear(date);
    }

    private void addRawTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int distance, int duration, int storageIndex) {
        storageRow[storageIndex] = StatCalculator.CalculateRawTrend(date, rawDataMap, distance, duration);
    }

    private void addVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int duration, int storageIndex) {
        storageRow[storageIndex] = StatCalculator.CalcluateVelocity(date, rawDataMap, duration);
    }

    private boolean hasNull(Object[] row) {
        for (Object o : row) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    private void addSituationClass(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int distance, int duration, int storageIndex, double significance) {
        storageRow[storageIndex] = StatCalculator.CalcluateSituation(date, rawDataMap, distance, duration, significance);
    }

}
