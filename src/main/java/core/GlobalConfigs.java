/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import datapreparer.CSVDownloader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * global configuration class
 */
public class GlobalConfigs {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // Global Value:  project path
    public static final String DEFAULT_PATH = "D:\\Documents\\NetBeansProjects\\ProjectSPA\\";

    public static final int TRAINING_TYPES_SIZE = TRAINIG_TYPES.values().length;

    public static final ArrayList<String> INSTRUMENT_CODES = loadInstrumentCodes();

    public static enum TRAINIG_TYPES {

        RAWTREND_0d(0),
        RAWTREND_1d(1),
        RAWTREND_3d(2),
        RAWTREND_1w(3),
        RAWTREND_2w(4),
        RAWTREND_4w(5),
        RAWTREND_7w(6),
        RAWTREND_9w(7),
        RAWTREND_12w(8),
        SEASON_YEAR(9),
        VELOCITY_0d(10),
        FUTTREND_7d(11),
        FUTTREND_14d(12),
        FUTTREND_28d(13),
        FUTTREND_49d(14);
        private int value;

        private static final int size = TRAINIG_TYPES.values().length;

        private TRAINIG_TYPES(int value) {
            this.value = value;
        }

        public int index() {
            return this.value;
        }

        public int size() {
            return size;
        }

    }

    private static ArrayList<String> loadInstrumentCodes() {
        String listpath = DEFAULT_PATH + "instrument_list.txt";
        ArrayList<String> codes = new ArrayList();
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(listpath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                codes.add(line);

            }
        } catch (FileNotFoundException fe) {
            Logger.getLogger(CSVDownloader.class
                    .getName()).log(Level.SEVERE, listpath + " does not exist", fe);
        } catch (Exception e) {
            Logger.getLogger(CSVDownloader.class
                    .getName()).log(Level.SEVERE, " error when load InstrumentCodes", e);
        }
        System.out.println(count + " instruments loaded.");
        return codes;
    }

}
