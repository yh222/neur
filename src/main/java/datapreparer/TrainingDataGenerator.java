package datapreparer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import core.GlobalConfigs;
import core.GlobalConfigs.*;
import static core.GlobalConfigs.DATE_FORMAT;
import static core.GlobalConfigs.TRAINING_TYPES_SIZE;
import org.encog.Encog;
import org.encog.app.analyst.AnalystFileFormat;
import org.encog.app.analyst.EncogAnalyst;
import org.encog.app.analyst.commands.CmdReset;
import org.encog.app.analyst.csv.normalize.AnalystNormalizeCSV;
import org.encog.app.analyst.script.normalize.AnalystField;
import org.encog.app.analyst.script.prop.ScriptProperties;
import org.encog.app.analyst.wizard.AnalystWizard;
import org.encog.util.csv.CSVFormat;

/**
 * Generate training data by raw .csv data downloaded by CSVDownloader
 *
 * Each data set stored as an ArrayList of double arrays
 */
public class TrainingDataGenerator {

    private String currentCode = "empty";

    static String RESOURCE_PATH = GlobalConfigs.DEFAULT_PATH + "resources\\";
    static ConcurrentHashMap<String, ArrayList> _trainingDataMap = new ConcurrentHashMap();

    public void generateTrainingData(boolean writeToFile, boolean writeToMomory, boolean normalize) {
        ArrayList<String> instruments = GlobalConfigs.INSTRUMENT_CODES;
        System.out.println("Starting to generate training data.");
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
        addSeasonOfYear(date, rawDataMap, storageRow, TRAINIG_TYPES.SEASON_YEAR.index());

        //Output data
        addCalssValues(date, rawDataMap, storageRow);

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

    /*
     * distance: distance from input date
     */
    private void addRawTrend(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int distance, int duration, int storageIndex) {

        Calendar start_date = getUsableDate(date, rawDataMap, distance, duration, true);
        Calendar end_date = getUsableDate(date, rawDataMap, distance, duration, false);
        if (start_date == null || end_date == null) {
            return; //remain 0
        }

        double trend_start = (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[0];
        double trend_end = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[3];
        storageRow[storageIndex] = (trend_end - trend_start) / trend_start;

    }

    private Calendar getUsableDate(String date, ConcurrentHashMap<String, Object[]> rawDataMap, int distance, int duration, boolean isStart) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));

            int direction;
            if (isStart) {
                direction = 1;
            } else {
                tempdate.add(Calendar.DAY_OF_MONTH, duration);
                direction = -1;
            }

            //Sundays and Saturdays will be ommited
            tempdate.add(Calendar.DAY_OF_MONTH, distance * -1);
            int count = 6;
            while (rawDataMap.get(DATE_FORMAT.format(tempdate.getTime())) == null) {
                tempdate.add(Calendar.DAY_OF_MONTH, direction);
                if (--count < 0) {
                    //System.out.println("Cannot find data on date:" + tempdate.getTime() + " for code: " + currentCode + "\n Initial date string: " + date + ", direction:" + direction);
                    return null;
                }
            }
            return tempdate;
        } catch (ParseException ex) {
            Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*
     * Summer = 1, Spring = 0.5, Autumn = -0.5, Winter = -1
     */
    private void addSeasonOfYear(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int storageIndex) {
        try {
            Calendar tempdate = Calendar.getInstance();
            tempdate.setTime(DATE_FORMAT.parse(date));
            int month = tempdate.get(Calendar.MONTH);
            if (month <= 2) {
                storageRow[storageIndex] = "Spring";
            } else if (month > 2 && month <= 5) {
                storageRow[storageIndex] = "Summer";
            } else if (month > 5 && month <= 8) {
                storageRow[storageIndex] = "Autumn";
            } else {
                storageRow[storageIndex] = "Winter";
            }
        } catch (ParseException ex) {
            Logger.getLogger(TrainingDataGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void addVelocities(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        // 21 day (plus weekends) duration NASDAQ velocity
        addVelocity(date, rawDataMap, storageRow, 30, TRAINIG_TYPES.VELOCITY_0d.index());
    }

    /*
     * Velocity = volume of the date / Average volume
     */
    private void addVelocity(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow, int duration, int storageIndex) {
        Calendar start_date = getUsableDate(date, rawDataMap, duration, 0, true);
        Calendar end_date = getUsableDate(date, rawDataMap, 0, 0, false);
        if (start_date == null || end_date == null) {
            return; //remain 0
        }
        //System.out.println("start: " + DATE_FORMAT.format(start_date.getTime()));
        //System.out.println("end: " + DATE_FORMAT.format(end_date.getTime()));
        int count = 0;
        double sum_volume = 0;
        //Calculate volume sum
        for (int i = 0; i < duration; i++) {
            if (rawDataMap.get(DATE_FORMAT.format(start_date.getTime())) != null) {
                count++;
                sum_volume += (double) rawDataMap.get(DATE_FORMAT.format(start_date.getTime()))[4];
                //System.out.println(DATE_FORMAT.format(start_date.getTime()));
            } else {
                //System.out.println("cannot find: " + DATE_FORMAT.format(start_date.getTime()));
            }
            start_date.add(Calendar.DATE, 1);
        }
        double current_volume = (double) rawDataMap.get(DATE_FORMAT.format(end_date.getTime()))[4];
        //System.out.println("current vol: " + current_volume + " sum vol: " + sum_volume + " count: " + count);
        storageRow[storageIndex] = current_volume / (sum_volume / count);
    }

    private void addCalssValues(String date, ConcurrentHashMap<String, Object[]> rawDataMap, Object[] storageRow) {
        addRawTrend(date, rawDataMap, storageRow, -7, 0, TRAINIG_TYPES.FUTTREND_7d.index());
        addRawTrend(date, rawDataMap, storageRow, -14, 0, TRAINIG_TYPES.FUTTREND_14d.index());
        addRawTrend(date, rawDataMap, storageRow, -24, 0, TRAINIG_TYPES.FUTTREND_28d.index());
        addRawTrend(date, rawDataMap, storageRow, -49, 0, TRAINIG_TYPES.FUTTREND_49d.index());
    }

    private boolean removeEarilyRows(String date, ConcurrentHashMap<String, Object[]> rawDataMap) {
        //If this entry has no data value 31 days ago
        Calendar tempdate = getUsableDate(date, rawDataMap, 31, 0, true);
        if (tempdate == null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean hasNull(Object[] row) {
        for (Object o : row) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

}
