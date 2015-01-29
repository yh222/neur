package datapreparer;

import datapreparer.valuemaker.ClassValueMaker;
import datapreparer.valuemaker.TrainingValueMaker;
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
import static core.GlobalConfigs.TRAINING_VALUES_SIZE;
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

                Object[] temp_training = new Object[TRAINING_VALUES_SIZE];
                TrainingValueMaker.generateTrainingValues(code, date, raw_data_map, temp_training);
                Object[] temp_class = new Object[CLASS_VALUES_SIZE];
                ClassValueMaker.generateCalssValues(code, date, raw_data_map, temp_class);

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

    /*
     * Check if a data row contains null value
     */
    private boolean hasNull(Object[] row) {
        for (Object o : row) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }
}
