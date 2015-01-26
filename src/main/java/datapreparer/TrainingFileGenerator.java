package datapreparer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import core.GlobalConfigs;
import core.GlobalConfigs.*;
import static core.GlobalConfigs.CLASS_VALUES_SIZE;
import static core.GlobalConfigs.TRAINIG_VALUES_SIZE;
import static datapreparer.RawDataLoader.loadRawDataFromFile;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of double arrays
 */
public class TrainingFileGenerator {

    static String RESOURCE_PATH = GlobalConfigs.DEFAULT_PATH + "resources\\";
    static ConcurrentHashMap<String, ArrayList> m_TrainingDataMap = new ConcurrentHashMap();
    private final double SIGNIFICANCE_NORMAL = 0.05;
    private final double SIGNIFICANCE_HIGH = 0.010;

    public void generateTrainingData(boolean writeToFile, boolean writeToMomory, boolean createHeaders) {
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

                Object[] temp_training = new Object[TRAINIG_VALUES_SIZE];
                generateTrainingValues(date, raw_data_map, temp_training);
                Object[] temp_class = new Object[CLASS_VALUES_SIZE];
                generateCalssValues(date, raw_data_map, temp_class);

                //remove line with null value
                //This is for bad entries, not for missing values. Missing values should be filled rather than leave null
                if (hasNull(temp_training) || hasNull(temp_class)) {
                    continue;
                }
                temp_data.add(ArrayUtils.addAll(temp_training, temp_class));
            }

            //Data processing end, start to save results.
            if (writeToMomory) {
                m_TrainingDataMap.put(code, temp_data);
            }
            if (writeToFile) {
                File training_file = new File(RESOURCE_PATH + code + "//" + code + "_Training.csv");
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(training_file, false)))) {
                    String temp = "";
                    if (createHeaders) {
                        for (TRAINING_VALUES v : TRAINING_VALUES.values()) {
                            temp += v.toString() + ",";
                        }
                        for (CLASS_VALUES v : CLASS_VALUES.values()) {
                            temp += v.toString() + ",";
                        }
                        writer.println(temp.substring(0, temp.length() - 1));
                    }
                    for (Object[] row : temp_data) {
                        temp = "";
                        for (Object item : row) {
                            temp += (item + ",");
                        }
                        //remove last comma and write to file
                        writer.println(temp.substring(0, temp.length() - 1));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TrainingFileGenerator.class.getName()).log(Level.SEVERE, "Failed to write to training file for: " + code, ex);
                }
            }
        }
        System.out.println("Training data successfully generated.");
    }

    // storageRow will store new items created
    private void generateTrainingValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        //Input data
        addRawTrends(date, rawDataMap, storageRow);
        addVelocities(date, rawDataMap, storageRow);
        addSeasonOfYear(date, storageRow, TRAINING_VALUES.SEASON_YEAR.index());
    }

    private void generateCalssValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addRawTrend(date, rawDataMap, storageRow, 0, 6, CLASS_VALUES.FUTTREND_7d.index());
        addRawTrend(date, rawDataMap, storageRow, 0, 13, CLASS_VALUES.FUTTREND_14d.index());
        addRawTrend(date, rawDataMap, storageRow, 0, 27, CLASS_VALUES.FUTTREND_28d.index());
        addRawTrend(date, rawDataMap, storageRow, 0, 48, CLASS_VALUES.FUTTREND_49d.index());

        addSituationClass(date, rawDataMap, storageRow, 0, 6, CLASS_VALUES.FUTSITU_7d.index(), SIGNIFICANCE_NORMAL);
        
        
        
        
    }

    private void addRawTrends(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addRawTrend(date, rawDataMap, storageRow, 0, 0, TRAINING_VALUES.RAWTREND_0d.index());
        addRawTrend(date, rawDataMap, storageRow, 1, 0, TRAINING_VALUES.RAWTREND_1d.index());
        addRawTrend(date, rawDataMap, storageRow, 3, 3, TRAINING_VALUES.RAWTREND_3d.index());
        addRawTrend(date, rawDataMap, storageRow, 7, 7, TRAINING_VALUES.RAWTREND_1w.index());
        addRawTrend(date, rawDataMap, storageRow, 14, 14, TRAINING_VALUES.RAWTREND_2w.index());
        addRawTrend(date, rawDataMap, storageRow, 28, 28, TRAINING_VALUES.RAWTREND_4w.index());
        addRawTrend(date, rawDataMap, storageRow, 49, 49, TRAINING_VALUES.RAWTREND_7w.index());
        addRawTrend(date, rawDataMap, storageRow, 63, 63, TRAINING_VALUES.RAWTREND_9w.index());
        addRawTrend(date, rawDataMap, storageRow, 72, 72, TRAINING_VALUES.RAWTREND_12w.index());
    }

    private void addVelocities(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        // 21 day (plus weekends) duration NASDAQ velocity
        addVelocity(date, rawDataMap, storageRow, 30, TRAINING_VALUES.VELOCITY_0d.index());
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
