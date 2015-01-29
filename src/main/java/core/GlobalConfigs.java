/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import datapreparer.CSVDownloader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * global configuration class
 */
public class GlobalConfigs {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // Global Value:  project path
    public static final String DEFAULT_PATH = "D:\\Documents\\NetBeansProjects\\ProjectSPA\\";

    public static final ArrayList<String> INSTRUMENT_CODES = loadInstrumentCodes();

    public static final int TRAINING_VALUES_SIZE = TRAINING_VALUES.values().length;
    public static final int CLASS_VALUES_SIZE = CLASS_VALUES.values().length;

    private static ConcurrentHashMap<String, Double> NormalSigMap;

    public static final double getSignificanceNormal(String code) {
        return 0.04;
//        if (NormalSigMap == null) {
//            loadSignificanceMaps();
//        } else if (NormalSigMap.containsKey(code)) {
//            return NormalSigMap.get(code);
//        } else {
//            
//        }
    }

    private static ConcurrentHashMap<String, Double> DailySigMap;

    public static final double getSignificanceDaily(String code) {
        return 0.015;
//        if (DailySigMap == null) {
//            loadSignificanceMaps();
//        } else if (DailySigMap.containsKey(code)) {
//            return DailySigMap.get(code);
//        } else {
//
//        }
    }

    private static void loadSignificanceMaps() {
        ObjectInputStream inputStream = null;
        try {
            String fileName = DEFAULT_PATH + "resources\\NormalSigMap.bin";
            inputStream = new ObjectInputStream(new FileInputStream(fileName));
            NormalSigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
            fileName = DEFAULT_PATH + "resources\\DailySigMap.bin";
            inputStream = new ObjectInputStream(new FileInputStream(fileName));
            DailySigMap = (ConcurrentHashMap<String, Double>) inputStream.readObject();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
            NormalSigMap = new ConcurrentHashMap();
            DailySigMap = new ConcurrentHashMap();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(GlobalConfigs.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static enum TRAINING_VALUES {

        //
        CLUTRENDHIGH_7d,
        CLUTRENDHIGH_14d,
        CLUTRENDHIGH_28d,
        CLUTRENDHIGH_49d,
        CLUTRENDHIGH_63d,
        CLUTRENDHIGH_84d,
        //
        CLUTRENDLOW_7d,
        CLUTRENDLOW_14d,
        CLUTRENDLOW_28d,
        CLUTRENDLOW_49d,
        CLUTRENDLOW_63d,
        CLUTRENDLOW_84d,
        //
        PASTLOW_7d,
        PASTLOW_14d,
        PASTLOW_28d,
        PASTLOW_49d,
        PASTLOW_63d,
        PASTLOW_84d,
        //
        PASTHIGH_7d,
        PASTHIGH_14d,
        PASTHIGH_28d,
        PASTHIGH_49d,
        PASTHIGH_63d,
        PASTHIGH_84d,
        //
        RAWTREND_0d,
        RAWTREND_1d,
        RAWTREND_3d,
        RAWTREND_7d,
        RAWTREND_14d,
        RAWTREND_28d,
        RAWTREND_49d,
        RAWTREND_63d,
        RAWTREND_84d,
        //
        DAY_OF_WEEK,
        SEASON_YEAR,
        VELOCITY_15d,
        VELOCITY_30d,
        //
        GreatW_7d,
        GreadB_7d,
        LongW_7d,
        LongB_7d,
        ShortW_7d,
        ShortB_7d,
        ShortWTT_7d,
        ShortBTT_7d,
        ShortWU_7d,
        ShortWD_7d,
        ShortBU_7d,
        ShortBD_7d,
        StarW_7d,
        StarB_7d,
        StarWTT_7d,
        StarBTT_7d,
        StarWU_7d,
        StarWD_7d,
        StarBU_7d,
        StarBD_7d,
        //
        GreatW_15d,
        GreadB_15d,
        LongW_15d,
        LongB_15d,
        ShortW_15d,
        ShortB_15d,
        ShortWTT_15d,
        ShortBTT_15d,
        ShortWU_15d,
        ShortWD_15d,
        ShortBU_15d,
        ShortBD_15d,
        StarW_15d,
        StarB_15d,
        StarWTT_15d,
        StarBTT_15d,
        StarWU_15d,
        StarWD_15d,
        StarBU_15d,
        StarBD_15d,
        //
        GreatW_30d,
        GreadB_30d,
        LongW_30d,
        LongB_30d,
        ShortW_30d,
        ShortB_30d,
        ShortWTT_30d,
        ShortBTT_30d,
        ShortWU_30d,
        ShortWD_30d,
        ShortBU_30d,
        ShortBD_30d,
        StarW_30d,
        StarB_30d,
        StarWTT_30d,
        StarBTT_30d,
        StarWU_30d,
        StarWD_30d,
        StarBU_30d,
        StarBD_30d,
        //
        GreatW_60d,
        GreadB_60d,
        LongW_60d,
        LongB_60d,
        ShortW_60d,
        ShortB_60d,
        ShortWTT_60d,
        ShortBTT_60d,
        ShortWU_60d,
        ShortWD_60d,
        ShortBU_60d,
        ShortBD_60d,
        StarW_60d,
        StarB_60d,
        StarWTT_60d,
        StarBTT_60d,
        StarWU_60d,
        StarWD_60d,
        StarBU_60d,
        StarBD_60d,
        //
        CadRead_1d,
        CadRead_2d,
        CadRead_3d,
        CadRead_4d,
        CadRead_5d,
        CadRead_6d,
        CadRead_7d,
        CadRead_8d,
        CadRead_9d,
        CadRead_10d,
        CadRead_11d,
        CadRead_12d,
        CadRead_13d,
        CadRead_14d,
        CadRead_15d,
        CadRead_16d,
        CadRead_17d,
        CadRead_18d
        ;

        public static final int SIZE = TRAINING_VALUES.values().length;
    }

    public static enum CLASS_VALUES {

        //
        FUTCLUTRENDHIGH_7d,
        FUTCLUTRENDHIGH_14d,
        FUTCLUTRENDHIGH_28d,
        FUTCLUTRENDHIGH_49d,
        FUTCLUTRENDHIGH_63d,
        FUTCLUTRENDHIGH_84d,
        //
        FUTCLUTRENDLOW_7d,
        FUTCLUTRENDLOW_14d,
        FUTCLUTRENDLOW_28d,
        FUTCLUTRENDLOW_49d,
        FUTCLUTRENDLOW_63d,
        FUTCLUTRENDLOW_84d,
        //
        FUTHIGHEST_7d,
        FUTHIGHEST_14d,
        FUTHIGHEST_28d,
        FUTHIGHEST_49d,
        FUTHIGHEST_63d,
        FUTHIGHEST_84d,
        //
        FUTLOWEST_7d,
        FUTLOWEST_14d,
        FUTLOWEST_28d,
        FUTLOWEST_49d,
        FUTLOWEST_63d,
        FUTLOWEST_84d;
        //private int value;
        public static final int SIZE = CLASS_VALUES.values().length;

        public int appendedIndex() {
            return this.ordinal() + TRAINING_VALUES.values().length;
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
