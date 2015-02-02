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
import static core.GlobalConfigs.RESOURCE_PATH;
import static datapreparer.RawDataLoader.loadRawDataFromFile;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of double arrays
 */
public class TrainingFileGenerator {


    static ConcurrentHashMap<String, ArrayList> m_TrainingDataMap = new ConcurrentHashMap();

    public void generateTrainingData(boolean writeToFile, boolean writeToMomory, boolean createHeaders) {
        ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
        System.out.println("Starting to generate training data.");
        //The code currently being processed
        String currentCode;
        for (String code : instruments) {
            currentCode = code;
            //Get raw data organized by date for each instrument
            ConcurrentHashMap<String, Object[]> raw_data_map = loadRawDataFromFile(code);
            ArrayList<Object[]> temp_data = new ArrayList();
            TrainingValueMaker tvmaker = new TrainingValueMaker(code);
            Object[] nominal_training, numeric_training, class_value;
            //Start data processing
            for (String date : raw_data_map.keySet()) {
                nominal_training = new Object[TRAINING_VALUES_NOMINAL.SIZE];
                tvmaker.generateNominalTrainingValues(date, raw_data_map, nominal_training);
                numeric_training = new Object[TRAINING_VALUES_NUMERIC.SIZE];
                tvmaker.generateNumericTrainingValues(date, raw_data_map, numeric_training);
                class_value = new Object[CLASS_VALUES.SIZE];
                ClassValueMaker.generateCalssValues(code, date, raw_data_map, class_value);

                //remove line with null value
                //This is for bad entries, not for missing values. Missing values should be filled rather than leave null
                if (hasNull(nominal_training) || hasNull(numeric_training) || hasNull(class_value)) {
                    continue;
                }
                temp_data.add(jointArray(nominal_training, numeric_training, class_value));
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
                        for (TRAINING_VALUES_NOMINAL v : TRAINING_VALUES_NOMINAL.values()) {
                            temp += v.toString() + ",";
                        }
                        for (TRAINING_VALUES_NUMERIC v : TRAINING_VALUES_NUMERIC.values()) {
                            temp += v.toString() + ",";
                        }
                        for (CLASS_VALUES v : CLASS_VALUES.values()) {
                            temp += v.toString() + ",";
                        }
                        writer.println(temp.substring(0, temp.length() - 1));
                    }
                    String arff = ArffBuilder.covenertArff(RESOURCE_PATH + code + "//" + code + "_Training.csv", currentCode);
                    PrintWriter arffWriter = new PrintWriter(new BufferedWriter(
                            new FileWriter(arff, true)));

                    for (Object[] row : temp_data) {
                        temp = "";
                        for (Object item : row) {
                            temp += (item + ",");
                        }
                        //remove last comma and write to file
                        writer.println(temp.substring(0, temp.length() - 1));
                        arffWriter.println(temp.substring(0, temp.length() - 1));
                    }
                    arffWriter.flush();
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

    private Object[] jointArray(Object[] array1, Object[] array2, Object[] array3) {
        Object[] r = new Object[array1.length + array2.length + array3.length];

        System.arraycopy(array1, 0, r, 0, array1.length);
        System.arraycopy(array2, 0, r, array1.length, array2.length);
        System.arraycopy(array3, 0, r, array1.length + array2.length, array3.length);

        return r;
    }
}
